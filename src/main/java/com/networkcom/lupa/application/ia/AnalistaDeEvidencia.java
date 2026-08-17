package com.networkcom.lupa.application.ia;

import com.networkcom.lupa.domain.auditoria.MedicionSitio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networkcom.lupa.domain.auditoria.Dictamen;
import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.Senal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Le pide a la IA que dictamine las señales que se pueden deducir de la evidencia.
 *
 * La IA nunca calcula el puntaje: solo lee la evidencia y dice, con fundamento,
 * si cada señal se cumple. El puntaje lo sigue haciendo el motor con reglas
 * fijas. Es la división que permite que dos auditorías del mismo comercio den
 * el mismo número aunque el modelo redacte distinto.
 */
@Service
public class AnalistaDeEvidencia {

    private static final Logger log = LoggerFactory.getLogger(AnalistaDeEvidencia.class);

    /**
     * Cuántas señales entran en cada consulta.
     *
     * Este número es una decisión de costo, no de velocidad, y lo aprendí
     * midiendo. Partirlo en tandas de 5 bajaba el análisis de 14 a 9 segundos,
     * pero la capa gratuita de Gemini permite **5 consultas por minuto y 20 por
     * día**: con 6 consultas por auditoría se agota el día entero en tres
     * comercios, y las tandas que no entran se pierden en silencio.
     *
     * Con 20 no se parte ningún grupo: quedan 3 consultas por auditoría, una
     * por fuente de evidencia. Se pagan unos segundos más a cambio de que la
     * herramienta sirva para una jornada de prospección completa.
     */
    private static final int SENALES_POR_CONSULTA = 20;

    private static final String INSTRUCCIONES = """
            Sos un auditor de presencia digital de comercios locales argentinos.

            Vas a recibir la evidencia de un comercio y una lista de señales a evaluar.
            Para cada señal respondé si se cumple, no se cumple, o no aplica a ese rubro.

            Reglas que no podés romper:
            - Basate solamente en la evidencia. Si algo no está en la evidencia, no lo supongas.
            - Cuando la evidencia no alcanza para decidir, usá confianza BAJA y decilo en el fundamento.
            - El fundamento tiene que citar lo que viste, no repetir la pregunta. Máximo 130 caracteres.
            - Usá NO_APLICA solo si la señal no tiene sentido para ese rubro, no si falta información.
            - Escribí en español rioplatense, sin tratar de usted.

            Devolvé únicamente un array JSON, sin texto alrededor y sin bloques de código:
            [{"codigo":"G_HORARIOS","estado":"CUMPLE","confianza":"ALTA","fundamento":"La ficha muestra horarios de lunes a sábado."}]

            Los valores de estado son CUMPLE, NO_CUMPLE o NO_APLICA.
            Los de confianza son ALTA, MEDIA o BAJA.
            """;

    private final ProveedorIA proveedor;
    private final ObjectMapper json;

    public AnalistaDeEvidencia(ProveedorIA proveedor, ObjectMapper json) {
        this.proveedor = proveedor;
        this.json = json;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaCruda(String codigo, String estado, String confianza, String fundamento) {
    }

    /**
     * Devuelve un dictamen por cada señal que la IA pudo evaluar.
     *
     * Si el modelo falla o contesta cualquier cosa, se devuelve un mapa vacío y
     * esas señales caen al cuestionario manual. Nunca se inventa una respuesta:
     * es preferible que el auditor conteste siete preguntas más a que el informe
     * afirme algo que nadie verificó.
     */
    /**
     * Lo que devolvió el análisis, con un aviso cuando algo salió mal.
     *
     * El aviso existe porque quedarse sin cuota y que el modelo no encuentre
     * nada se ven idénticos desde la pantalla: en los dos casos no aparecen
     * dictámenes. Sin explicación, el auditor concluye que la IA no sirve.
     */
    public record ResultadoAnalisis(Map<Senal, Dictamen> dictamenes, String aviso) {

        static ResultadoAnalisis vacio() {
            return new ResultadoAnalisis(Map.of(), null);
        }
    }

    public ResultadoAnalisis analizar(Evidencia evidencia) {
        List<Senal> aEvaluar = senalesEvaluables(evidencia);

        if (aEvaluar.isEmpty()) {
            return ResultadoAnalisis.vacio();
        }

        List<Map.Entry<FuenteDeEvidencia, List<Senal>>> tandas = aEvaluar.stream()
                .collect(Collectors.groupingBy(FuenteDeEvidencia::de))
                .entrySet().stream()
                .flatMap(grupo -> partir(grupo.getValue(), SENALES_POR_CONSULTA).stream()
                        .map(tanda -> Map.entry(grupo.getKey(), tanda)))
                .toList();

        long comienzo = System.nanoTime();
        Map<Senal, Dictamen> dictamenes = new EnumMap<>(Senal.class);

        /*
         * Una consulta por grupo, todas en paralelo.
         *
         * Antes iban las 27 señales en una sola consulta y tardaba unos 18
         * segundos. Partidas por fuente, cada consulta es más corta y además
         * lleva solo la evidencia que le corresponde: al grupo de Google no le
         * hace falta el texto de Instagram. El total pasa a ser el de la
         * consulta más lenta, no la suma.
         *
         * El otro beneficio importa igual o más: si una falla, las otras
         * llegan. Antes, un error dejaba las 27 señales sin responder.
         *
         * Hilos virtuales porque esto es puro esperar respuestas de red: son
         * baratos de crear y no hay que dimensionar ningún pool.
         */
        List<String> avisos = Collections.synchronizedList(new ArrayList<>());

        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Map<Senal, Dictamen>>> tareas = tandas.stream()
                    .map(tanda -> ejecutor.submit(
                            () -> analizarGrupo(evidencia, tanda.getKey(), tanda.getValue(), avisos)))
                    .toList();

            for (Future<Map<Senal, Dictamen>> tarea : tareas) {
                try {
                    dictamenes.putAll(tarea.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException e) {
                    log.warn("Una tanda de señales falló: {}", e.getCause().getMessage());
                }
            }
        }

        log.info("Analizadas {} de {} señales en {} ms, en {} consultas",
                dictamenes.size(), aEvaluar.size(),
                (System.nanoTime() - comienzo) / 1_000_000, tandas.size());

        return new ResultadoAnalisis(dictamenes, resumirAvisos(avisos, dictamenes.size(), aEvaluar.size()));
    }

    /**
     * Un solo aviso para el auditor, aunque hayan fallado varias tandas.
     *
     * Se prioriza el del límite de cuota porque es accionable —hay que esperar—
     * mientras que los demás fallos solo justifican reintentar.
     */
    private String resumirAvisos(List<String> avisos, int resueltas, int pedidas) {
        if (resueltas == pedidas) {
            return null;
        }

        String faltantes = " Quedaron " + (pedidas - resueltas) + " señales para responder a mano.";

        if (avisos.isEmpty()) {
            // Ninguna consulta falló pero el modelo devolvió menos señales de las
            // pedidas. Pasa con los modelos livianos y no lanza ningún error, así
            // que sin este caso el análisis quedaba corto sin decir una palabra.
            return "La IA no llegó a evaluar todas las señales." + faltantes;
        }

        return avisos.stream()
                .filter(aviso -> aviso.contains("límite"))
                .findFirst()
                .map(aviso -> aviso + faltantes)
                .orElse("El análisis con IA falló en parte." + faltantes);
    }

    private static <T> List<List<T>> partir(List<T> lista, int tamano) {
        List<List<T>> tandas = new ArrayList<>();
        for (int desde = 0; desde < lista.size(); desde += tamano) {
            tandas.add(lista.subList(desde, Math.min(desde + tamano, lista.size())));
        }
        return tandas;
    }

    private Map<Senal, Dictamen> analizarGrupo(Evidencia evidencia, FuenteDeEvidencia fuente,
                                               List<Senal> senales, List<String> avisos) {
        try {
            String respuesta = proveedor.responderJson(INSTRUCCIONES, armarContenido(evidencia, fuente, senales));
            return interpretar(respuesta, senales);
        } catch (RuntimeException e) {
            log.warn("El análisis de {} falló, esas señales quedan para el cuestionario: {}",
                    fuente, e.getMessage());
            avisos.add(e.getMessage());
            return Map.of();
        }
    }

    /** Qué señales tiene sentido preguntarle a la IA con la evidencia disponible. */
    public static List<Senal> senalesEvaluables(Evidencia evidencia) {
        List<Senal> evaluables = new ArrayList<>();

        for (Senal senal : Senal.values()) {
            boolean puede = switch (FuenteDeEvidencia.de(senal)) {
                case GOOGLE -> evidencia.tieneFichaGoogle();
                case INSTAGRAM -> evidencia.tieneInstagram();
                case CRUZADA -> evidencia.tieneSitioMedido()
                        || evidencia.tieneInstagram()
                        || evidencia.tieneFichaGoogle();
                case SITIO, SOLO_HUMANO -> false;
            };

            if (puede) {
                evaluables.add(senal);
            }
        }

        return evaluables;
    }

    /**
     * Arma el contenido con la evidencia que ese grupo necesita y nada más.
     *
     * Mandarle el perfil de Instagram a la consulta que evalúa la ficha de
     * Google no aporta nada y encarece cada llamada. El grupo cruzado sí recibe
     * todo, porque su trabajo es justamente comparar canales entre sí.
     */
    private String armarContenido(Evidencia evidencia, FuenteDeEvidencia fuente, List<Senal> aEvaluar) {
        boolean incluirGoogle = fuente == FuenteDeEvidencia.GOOGLE || fuente == FuenteDeEvidencia.CRUZADA;
        boolean incluirInstagram = fuente == FuenteDeEvidencia.INSTAGRAM || fuente == FuenteDeEvidencia.CRUZADA;

        StringBuilder texto = new StringBuilder();

        texto.append("## Comercio\n")
                .append("Nombre declarado: ").append(valorOSinDato(evidencia.nombreDeclarado())).append('\n')
                .append("Rubro: ").append(valorOSinDato(evidencia.rubro())).append('\n')
                .append("Ciudad: ").append(valorOSinDato(evidencia.ciudad())).append('\n')
                .append("Teléfono declarado: ").append(valorOSinDato(evidencia.telefonoDeclarado())).append('\n')
                .append("Dirección declarada: ").append(valorOSinDato(evidencia.direccionDeclarada())).append('\n')
                .append("Sitio web: ").append(valorOSinDato(evidencia.sitioWeb())).append('\n');

        if (evidencia.tieneSitioMedido()) {
            var sitio = evidencia.medicionSitio();
            texto.append("\n## Medición del sitio\n")
                    .append("Título: ").append(valorOSinDato(sitio.titulo())).append('\n')
                    .append("Descripción: ").append(valorOSinDato(sitio.metaDescripcion())).append('\n')
                    .append("Enlaces de contacto visibles: ").append(sitio.tieneContactoVisible() ? "sí" : "no").append('\n')
                    .append("Adaptado a celular: ").append(sitio.tieneViewport() ? "sí" : "no").append('\n');
        }

        if (incluirGoogle && evidencia.tieneFichaGoogle()) {
            texto.append("\n## Ficha de Google, pegada por el auditor\n")
                    .append(evidencia.textoFichaGoogle().strip()).append('\n');
        }

        if (incluirInstagram && evidencia.tieneInstagram()) {
            texto.append("\n## Perfil de Instagram, pegado por el auditor\n")
                    .append(evidencia.textoInstagram().strip()).append('\n');
        }

        if (evidencia.notasDelAuditor() != null && !evidencia.notasDelAuditor().isBlank()) {
            texto.append("\n## Notas del auditor\n")
                    .append(evidencia.notasDelAuditor().strip()).append('\n');
        }

        texto.append("\n## Señales a evaluar\n");
        for (Senal senal : aEvaluar) {
            texto.append("- ").append(senal.name()).append(": ").append(senal.getPregunta()).append('\n');
        }

        return texto.toString();
    }

    private Map<Senal, Dictamen> interpretar(String respuesta, List<Senal> permitidas) {
        Map<Senal, Dictamen> dictamenes = new EnumMap<>(Senal.class);

        List<RespuestaCruda> crudas;
        try {
            crudas = json.readerForListOf(RespuestaCruda.class).readValue(extraerArray(respuesta));
        } catch (Exception e) {
            log.warn("La IA devolvió algo que no es JSON válido: {}", e.getMessage());
            return Map.of();
        }

        for (RespuestaCruda cruda : crudas) {
            Senal senal = aSenal(cruda.codigo());

            // Se descarta lo que no se preguntó: el modelo a veces agrega señales
            // de más, y aceptarlas sería dejar que decida qué se audita.
            if (senal == null || !permitidas.contains(senal)) {
                continue;
            }

            EstadoSenal estado = aEstado(cruda.estado());
            if (estado == null) {
                continue;
            }

            dictamenes.put(senal, Dictamen.deIa(
                    senal,
                    estado,
                    recortar(cruda.fundamento()),
                    aConfianza(cruda.confianza())));
        }

        return dictamenes;
    }

    /**
     * Rescata el array aunque venga envuelto en un bloque de código o con texto
     * de cortesía alrededor, que es lo que hacen los modelos por más que se les
     * pida lo contrario.
     */
    private String extraerArray(String respuesta) {
        String limpio = respuesta.strip();
        int desde = limpio.indexOf('[');
        int hasta = limpio.lastIndexOf(']');

        if (desde >= 0 && hasta > desde) {
            return limpio.substring(desde, hasta + 1);
        }
        return limpio;
    }

    private Senal aSenal(String codigo) {
        if (codigo == null) {
            return null;
        }
        try {
            return Senal.valueOf(codigo.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private EstadoSenal aEstado(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return EstadoSenal.valueOf(valor.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Dictamen.Confianza aConfianza(String valor) {
        if (valor == null) {
            return Dictamen.Confianza.BAJA;
        }
        try {
            return Dictamen.Confianza.valueOf(valor.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Dictamen.Confianza.BAJA;
        }
    }

    private String recortar(String fundamento) {
        if (fundamento == null) {
            return null;
        }
        String limpio = fundamento.strip();
        return limpio.length() <= 240 ? limpio : limpio.substring(0, 237) + "...";
    }

    private String valorOSinDato(String valor) {
        return valor == null || valor.isBlank() ? "(sin dato)" : valor.strip();
    }
}
