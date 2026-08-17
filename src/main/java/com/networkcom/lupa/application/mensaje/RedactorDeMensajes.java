package com.networkcom.lupa.application.mensaje;

import com.networkcom.lupa.application.ia.ProveedorIA;
import com.networkcom.lupa.domain.auditoria.Auditoria;
import com.networkcom.lupa.domain.auditoria.MotorDePuntaje;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Escribe el mensaje con el que se le manda el diagnóstico al prospecto.
 *
 * Usa la IA para redactarlo con los hallazgos concretos del comercio, pero
 * siempre hay una plantilla de respaldo que funciona sola. Y no es una plantilla
 * de descarte: si mañana se cae Gemini en medio de una jornada de prospección,
 * el auditor tiene que poder seguir mandando mensajes decentes.
 */
@Service
public class RedactorDeMensajes {

    private static final Logger log = LoggerFactory.getLogger(RedactorDeMensajes.class);

    /** Cuántos hallazgos se le pasan al modelo para que elija los que mejor entren. */
    private static final int HALLAZGOS_DE_CONTEXTO = 5;

    /**
     * El registro es el de un contacto comercial entre profesionales.
     *
     * Se prohíben expresamente los modismos porque el modelo, si le pedís
     * "español rioplatense", se va al extremo del "che" y el "te re": suena a
     * conocido mandando un audio, no a una agencia presentando un trabajo. La
     * cercanía tiene que salir de ser concreto y breve, no del vocabulario.
     */
    private static final String REGISTRO = """
            Registro: comercial y profesional, primer contacto con alguien que no
            te conoce.
            - Español de Argentina, tratamiento de USTED. Nunca tutees ni voseés.
            - Empezá siempre con un saludo cordial: "Buenos días" o "Buenas tardes".
              El saludo va solo, antes de presentarte. Nunca arranques diciendo
              quién sos ni yendo directo al problema.
            - Prohibido: che, boludo, "re" como intensificador, viste, posta,
              diminutivos, emojis y signos de exclamación.
            - Prohibido también el relleno de folleto: "quedamos a disposición",
              "no dude en consultar", "potenciar", "impulsar", "llevar su negocio
              al siguiente nivel".
            - Frases cortas y concretas. Cortés sin ser servil.
            """;

    private static final String INSTRUCCIONES_WHATSAPP = """
            Escribís el primer mensaje de WhatsApp con el que %s, de la agencia
            Networkcom, le acerca un diagnóstico sin cargo a un comercio local.

            %s

            Estructura, en este orden exacto:
            1. Un saludo cordial solo, en su propio renglón. Ejemplo: "Buenos días."
            2. Presentación: tu nombre y la agencia.
            3. UN hallazgo concreto de los que te paso y qué le cuesta
               comercialmente: turnos que no entran, clientes que no lo encuentran.
            4. Cierre ofreciendo el diagnóstico, sin presionar ni pedir reunión.

            - Máximo 5 renglones. Es WhatsApp, no un correo.
            - No prometas resultados, no hables de precios y no firmes al final.
            - Devolvé solo el texto del mensaje, sin comillas y sin explicaciones.
            - No incluyas el enlace: se agrega después automáticamente.
            """;

    private static final String INSTRUCCIONES_EMAIL = """
            Escribís el correo con el que %s, de la agencia Networkcom, le acerca
            un diagnóstico sin cargo a un comercio local.

            %s

            Estructura, en este orden exacto:
            1. El asunto, con el prefijo "Asunto: ". Máximo 60 caracteres, concreto,
               sin signos de exclamación.
            2. Una línea en blanco.
            3. El saludo cordial solo, en su propio renglón. Ejemplo: "Buenos días."
            4. Otra línea en blanco.
            5. El cuerpo, de 3 a 5 oraciones: presentación con tu nombre y la
               agencia, dos hallazgos concretos con lo que le cuestan
               comercialmente, y una línea sobre cómo se hizo el relevamiento para
               que no parezca spam.

            - No prometas resultados, no hables de precios y no firmes al final.
            - Devolvé solo asunto, saludo y cuerpo, sin explicaciones.
            - No incluyas el enlace: se agrega después automáticamente.
            """;

    private final ProveedorIA proveedor;

    public RedactorDeMensajes(ProveedorIA proveedor) {
        this.proveedor = proveedor;
    }

    public record Mensaje(String asunto, String cuerpo, boolean redactadoPorIa) {
    }

    public Mensaje redactar(Auditoria auditoria, CanalDeEnvio canal, String enlace) {
        var resultado = MotorDePuntaje.calcular(auditoria.respuestasParaElMotor());
        List<String> hallazgos = resultado.planDeAccion().stream()
                .limit(HALLAZGOS_DE_CONTEXTO)
                .map(MotorDePuntaje.Hallazgo::titulo)
                .toList();

        try {
            return conIa(auditoria, canal, enlace, hallazgos, resultado.puntajeGlobal());
        } catch (RuntimeException e) {
            log.info("El redactor cayó a la plantilla: {}", e.getMessage());
            return plantilla(auditoria, canal, enlace, hallazgos, resultado.puntajeGlobal());
        }
    }

    private Mensaje conIa(Auditoria auditoria, CanalDeEnvio canal, String enlace,
                          List<String> hallazgos, int puntaje) {

        String contexto = """
                Comercio: %s
                Rubro: %s
                Ciudad: %s
                Puntaje del diagnóstico: %d sobre 100
                Problemas detectados:
                %s
                """.formatted(
                auditoria.getNombre(),
                auditoria.getRubro(),
                auditoria.getCiudad() == null ? "(sin dato)" : auditoria.getCiudad(),
                puntaje,
                hallazgos.stream().map(h -> "- " + h).reduce("", (a, b) -> a + b + "\n").strip());

        String remitente = auditoria.getUsuario().getNombre();

        String instrucciones = (canal == CanalDeEnvio.WHATSAPP
                ? INSTRUCCIONES_WHATSAPP
                : INSTRUCCIONES_EMAIL).formatted(remitente, REGISTRO);

        String respuesta = proveedor.responder(instrucciones, contexto).strip();

        if (respuesta.isBlank()) {
            throw new ProveedorIA.ProveedorIAException("El modelo devolvió un mensaje vacío.");
        }

        if (canal == CanalDeEnvio.WHATSAPP) {
            return new Mensaje(null, respuesta + "\n\n" + enlace, true);
        }

        return separarAsunto(respuesta, enlace, auditoria);
    }

    /**
     * Parte la respuesta del modelo en asunto y cuerpo.
     *
     * Si no encuentra el prefijo pedido, usa un asunto propio en lugar de
     * mandar la primera línea como asunto: cuando el modelo se desvía, esa
     * línea suele ser una oración larga que queda pésima en la bandeja.
     */
    private Mensaje separarAsunto(String respuesta, String enlace, Auditoria auditoria) {
        String[] partes = respuesta.split("\\r?\\n", 2);
        String primera = partes[0].strip();

        if (primera.toLowerCase().startsWith("asunto:") && partes.length == 2) {
            String asunto = primera.substring("asunto:".length()).strip();
            String cuerpo = partes[1].strip();
            return new Mensaje(asunto, cuerpo + "\n\n" + enlace, true);
        }

        return new Mensaje(asuntoPorDefecto(auditoria), respuesta + "\n\n" + enlace, true);
    }

    private Mensaje plantilla(Auditoria auditoria, CanalDeEnvio canal, String enlace,
                              List<String> hallazgos, int puntaje) {

        String primero = hallazgos.isEmpty() ? null : hallazgos.get(0).toLowerCase();
        String remitente = auditoria.getUsuario().getNombre();

        if (canal == CanalDeEnvio.WHATSAPP) {
            String cuerpo = """
                    Buenos días.

                    Soy %s, de la agencia Networkcom. Revisamos cómo aparece %s cuando alguien lo busca por internet y encontramos algunos puntos para corregir%s.

                    Le dejo el diagnóstico completo por si quiere verlo:

                    %s"""
                    .formatted(
                            remitente,
                            auditoria.getNombre(),
                            primero == null ? "" : ", empezando por que " + primero,
                            enlace);
            return new Mensaje(null, cuerpo, false);
        }

        String cuerpo = """
                Buenos días.

                Soy %s, de la agencia Networkcom. Revisamos cómo aparece %s cuando un cliente lo busca por internet y armamos un diagnóstico con lo que encontramos.

                Hoy el negocio da %d sobre 100 en presencia digital%s. Ninguno de estos puntos es grave y la mayoría se corrige en poco tiempo.

                Le dejo el informe completo acá:

                %s"""
                .formatted(
                        remitente,
                        auditoria.getNombre(),
                        puntaje,
                        primero == null ? "" : ", sobre todo porque " + primero,
                        enlace);

        return new Mensaje(asuntoPorDefecto(auditoria), cuerpo, false);
    }

    private String asuntoPorDefecto(Auditoria auditoria) {
        return "Diagnóstico de presencia digital de " + auditoria.getNombre();
    }

    /**
     * Modismos que no corresponden en un primer contacto comercial.
     *
     * Existe para que una prueba pueda verificar que las plantillas de respaldo
     * no se vayan de registro. No filtra lo que devuelve el modelo: censurar su
     * salida a posteriori dejaría frases cortadas por la mitad, y para eso están
     * las instrucciones y la revisión del auditor antes de mandar.
     */
    public static final List<String> MODISMOS_PROHIBIDOS =
            List.of("che ", "boludo", " re ", "viste", "posta", "dale que");
}
