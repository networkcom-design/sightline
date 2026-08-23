package com.networkcom.lupa.application.auditoria;

import com.networkcom.lupa.application.auditoria.dto.PeticionEvidencia;
import com.networkcom.lupa.application.auditoria.dto.PeticionNuevaAuditoria;
import com.networkcom.lupa.application.auditoria.dto.RespuestasAuditoria;
import com.networkcom.lupa.application.ia.AnalistaDeEvidencia;
import com.networkcom.lupa.application.ia.Evidencia;
import com.networkcom.lupa.application.mensaje.CanalDeEnvio;
import com.networkcom.lupa.application.mensaje.RedactorDeMensajes;
import com.networkcom.lupa.domain.contacto.TelefonoArgentino;
import com.networkcom.lupa.domain.auditoria.Auditoria;
import com.networkcom.lupa.domain.auditoria.AuditoriaNoEncontradaException;
import com.networkcom.lupa.domain.auditoria.EstadoAuditoria;
import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.MotorDePuntaje;
import com.networkcom.lupa.domain.auditoria.RepositorioAuditorias;
import com.networkcom.lupa.domain.auditoria.Senal;
import com.networkcom.lupa.domain.propuesta.CatalogoServicios;
import com.networkcom.lupa.domain.propuesta.EstadoTarea;
import com.networkcom.lupa.domain.propuesta.GeneradorDePropuesta;
import com.networkcom.lupa.domain.usuario.Usuario;
import com.networkcom.lupa.infrastructure.web.AnalizadorSitioWeb;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class ServicioAuditorias {

    /** Cuántas prioridades se le muestran al prospecto en el enlace público. */
    private static final int PRIORIDADES_PUBLICAS = 5;

    private final RepositorioAuditorias repositorio;
    private final AnalizadorSitioWeb analizadorSitio;
    private final AnalistaDeEvidencia analistaIa;
    private final RedactorDeMensajes redactor;

    public ServicioAuditorias(RepositorioAuditorias repositorio,
                             AnalizadorSitioWeb analizadorSitio,
                             AnalistaDeEvidencia analistaIa,
                             RedactorDeMensajes redactor) {
        this.redactor = redactor;
        this.repositorio = repositorio;
        this.analizadorSitio = analizadorSitio;
        this.analistaIa = analistaIa;
    }

    /**
     * Crea la auditoría y mide el sitio en el momento.
     *
     * La medición se hace acá y no en segundo plano porque tarda uno o dos
     * segundos y es lo primero que el auditor quiere ver. Mandarla a una cola
     * agregaría complejidad para ahorrar un tiempo que nadie está esperando.
     */
    @Transactional
    public RespuestasAuditoria.Informe crear(Usuario usuario, PeticionNuevaAuditoria peticion) {
        Auditoria auditoria = Auditoria.iniciar(usuario, peticion.nombre(), peticion.rubro());
        auditoria.completarDatos(peticion.ciudad(), peticion.telefono(), peticion.direccion(), peticion.sitioWeb());

        var resultado = analizadorSitio.analizar(peticion.sitioWeb());
        auditoria.registrarMedicion(resultado.medicion(), resultado.senales());

        return armarInforme(repositorio.guardar(auditoria));
    }

    /**
     * Corrige los datos del comercio.
     *
     * Si cambió el sitio web se vuelve a medir en el momento. No hacerlo dejaría
     * nueve señales contestadas sobre una dirección que ya no es la del
     * comercio: el informe diría que el sitio carga rápido y es seguro, midiendo
     * uno que no es el suyo. Es peor que no tener el dato.
     */
    @Transactional
    public RespuestasAuditoria.Informe corregirDatos(
            Usuario usuario, UUID id, PeticionNuevaAuditoria peticion) {

        Auditoria auditoria = recuperar(usuario, id);

        boolean cambioElSitio = auditoria.corregirDatos(
                peticion.nombre(), peticion.rubro(), peticion.ciudad(),
                peticion.telefono(), peticion.direccion(), peticion.sitioWeb());

        if (cambioElSitio) {
            var resultado = analizadorSitio.analizar(peticion.sitioWeb());
            auditoria.registrarMedicion(resultado.medicion(), resultado.senales());
        }

        return armarInforme(repositorio.guardar(auditoria));
    }

    @Transactional(readOnly = true)
    public List<RespuestasAuditoria.Resumen> listar(Usuario usuario) {
        return repositorio.deUsuario(usuario).stream().map(RespuestasAuditoria.Resumen::de).toList();
    }

    /**
     * Guarda la evidencia pegada y la manda a analizar.
     *
     * Si la IA falla, los dictámenes vienen vacíos y esas señales quedan para el
     * cuestionario. La auditoría se guarda igual: la evidencia no se pierde
     * porque el modelo haya tenido un mal momento.
     */
    @Transactional
    public RespuestasAuditoria.Informe analizarEvidencia(Usuario usuario, UUID id, PeticionEvidencia peticion) {
        Auditoria auditoria = recuperar(usuario, id);
        auditoria.guardarEvidencia(peticion.textoInstagram(), peticion.textoFichaGoogle(), peticion.notas());

        Evidencia evidencia = new Evidencia(
                auditoria.getNombre(),
                auditoria.getRubro(),
                auditoria.getCiudad(),
                auditoria.getTelefono(),
                auditoria.getDireccion(),
                auditoria.getSitioWeb(),
                auditoria.getMedicion(),
                auditoria.getTextoInstagram(),
                auditoria.getTextoGoogle(),
                auditoria.getNotas());

        var analisis = analistaIa.analizar(evidencia);
        auditoria.registrarDictamenes(analisis.dictamenes());

        return armarInforme(repositorio.guardar(auditoria), analisis.aviso());
    }

    @Transactional
    public RespuestasAuditoria.Informe corregir(Usuario usuario, UUID id, String codigoSenal, EstadoSenal estado) {
        Auditoria auditoria = recuperar(usuario, id);

        Senal senal;
        try {
            senal = Senal.valueOf(codigoSenal.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuditoriaNoEncontradaException("No existe la señal " + codigoSenal + ".");
        }

        if (senal.isAutomatica()) {
            throw new SenalNoEditableException(senal);
        }

        auditoria.corregir(senal, estado);
        return armarInforme(repositorio.guardar(auditoria));
    }

    /**
     * Cambia si un servicio entra en el presupuesto y a qué precio.
     *
     * Se valida que el código exista en el catálogo: aceptar cualquiera dejaría
     * ajustes huérfanos que no se ven en ninguna pantalla y ensucian la
     * auditoría para siempre.
     */
    @Transactional
    public RespuestasAuditoria.Informe ajustarServicio(
            Usuario usuario, UUID id, String codigoServicio, boolean incluido, BigDecimal precio) {

        Auditoria auditoria = recuperar(usuario, id);

        boolean existe = CatalogoServicios.TODOS.stream()
                .anyMatch(servicio -> servicio.codigo().equals(codigoServicio));

        if (!existe) {
            throw new AuditoriaNoEncontradaException("No existe el servicio " + codigoServicio + ".");
        }

        auditoria.ajustarServicio(codigoServicio, incluido, precio);
        return armarInforme(repositorio.guardar(auditoria));
    }

    /**
     * Arma el mensaje para mandarle el diagnóstico al prospecto.
     *
     * No manda nada: devuelve el texto y los enlaces para que el auditor abra
     * WhatsApp o su correo y apriete enviar. Sightline no tiene ni necesita permiso
     * para escribirle a nadie en nombre de otro.
     */
    @Transactional(readOnly = true)
    public RespuestasAuditoria.MensajeParaEnviar mensaje(
            Usuario usuario, UUID id, CanalDeEnvio canal, String baseDelEnlace) {

        Auditoria auditoria = recuperar(usuario, id);

        if (!auditoria.sinResponder().isEmpty()) {
            throw new InformeIncompletoException(auditoria.sinResponder().size());
        }

        String enlace = baseDelEnlace + "/informe/" + auditoria.getTokenPublico();
        var mensaje = redactor.redactar(auditoria, canal, enlace);

        var telefono = TelefonoArgentino.paraWhatsApp(auditoria.getTelefono());

        String enlaceWhatsApp = telefono
                .map(numero -> "https://wa.me/" + numero + "?text="
                        + URLEncoder.encode(mensaje.cuerpo(), StandardCharsets.UTF_8))
                .orElse(null);

        return new RespuestasAuditoria.MensajeParaEnviar(
                canal.getEtiqueta(),
                mensaje.asunto(),
                mensaje.cuerpo(),
                enlace,
                telefono.orElse(null),
                telefono.map(TelefonoArgentino::paraMostrar).orElse(null),
                telefono.isPresent(),
                enlaceWhatsApp,
                mensaje.redactadoPorIa());
    }

    @Transactional
    public RespuestasAuditoria.Informe marcarEnviada(Usuario usuario, UUID id) {
        Auditoria auditoria = recuperar(usuario, id);
        auditoria.marcarEnviada();
        return armarInforme(repositorio.guardar(auditoria));
    }

    /**
     * El comercio aceptó el presupuesto.
     *
     * La propuesta se recalcula una última vez con los ajustes vigentes y esos
     * son los precios que quedan pactados. Se exige que el informe esté
     * completo por la misma razón que el enlace público: un presupuesto armado
     * sobre un diagnóstico a medias cotiza servicios que quizá no hagan falta.
     */
    @Transactional
    public RespuestasAuditoria.Informe aceptarPropuesta(Usuario usuario, UUID id) {
        Auditoria auditoria = recuperar(usuario, id);

        if (!auditoria.sinResponder().isEmpty()) {
            throw new InformeIncompletoException(auditoria.sinResponder().size());
        }

        var propuesta = GeneradorDePropuesta.generar(
                auditoria.respuestasParaElMotor(), CatalogoServicios.TODOS, auditoria.getAjustesPropuesta());

        auditoria.aceptarPropuesta(propuesta.items());
        return armarInforme(repositorio.guardar(auditoria));
    }

    @Transactional
    public RespuestasAuditoria.Informe rechazarPropuesta(Usuario usuario, UUID id, String motivo) {
        Auditoria auditoria = recuperar(usuario, id);
        auditoria.rechazarPropuesta(motivo);
        return armarInforme(repositorio.guardar(auditoria));
    }

    /** Mueve una tarea contratada. El estado del expediente se recalcula solo. */
    @Transactional
    public RespuestasAuditoria.Informe cambiarEstadoDeTarea(
            Usuario usuario, UUID id, String codigoServicio, EstadoTarea estado, String nota) {

        Auditoria auditoria = recuperar(usuario, id);

        if (!auditoria.getEstado().tieneTrabajoContratado()) {
            throw new SinTrabajoContratadoException();
        }

        auditoria.cambiarEstadoDeTarea(codigoServicio, estado, nota);
        return armarInforme(repositorio.guardar(auditoria));
    }

    @Transactional
    public RespuestasAuditoria.Informe marcarRevisada(Usuario usuario, UUID id) {
        Auditoria auditoria = recuperar(usuario, id);
        auditoria.marcarRevisada();
        return armarInforme(repositorio.guardar(auditoria));
    }

    @Transactional(readOnly = true)
    public RespuestasAuditoria.Informe informe(Usuario usuario, UUID id) {
        return armarInforme(recuperar(usuario, id));
    }

    @Transactional
    public void eliminar(Usuario usuario, UUID id) {
        repositorio.eliminar(recuperar(usuario, id));
    }

    /** La vista sin cuenta. La única credencial es el token del enlace. */
    @Transactional(readOnly = true)
    public RespuestasAuditoria.VistaPublica vistaPublica(String token) {
        Auditoria auditoria = repositorio.buscarPorToken(token)
                .orElseThrow(() -> new AuditoriaNoEncontradaException("Este enlace no corresponde a ningún informe."));

        // Un informe a medio completar no se le muestra a nadie. Con señales sin
        // responder el puntaje sale inflado, porque el motor solo evalúa lo que
        // se le contestó: mandarle eso a un prospecto sería mostrarle un
        // diagnóstico que dice que está mejor de lo que está.
        if (!auditoria.sinResponder().isEmpty()) {
            throw new InformeIncompletoException(auditoria.sinResponder().size());
        }

        var resultado = MotorDePuntaje.calcular(auditoria.respuestasParaElMotor());
        var propuesta = GeneradorDePropuesta.generar(
                auditoria.respuestasParaElMotor(), CatalogoServicios.TODOS, auditoria.getAjustesPropuesta());

        return new RespuestasAuditoria.VistaPublica(
                auditoria.getNombre(),
                auditoria.getRubro(),
                auditoria.getCiudad(),
                auditoria.getCreadaEn(),
                resultado.puntajeGlobal(),
                resultado.nivel(),
                resultado.nivel().getLectura(),
                RespuestasAuditoria.aDimensiones(resultado.dimensiones()),
                RespuestasAuditoria.aPrioridades(resultado.planDeAccion(), PRIORIDADES_PUBLICAS),
                propuesta.puntajeAlEntregar(),
                propuesta.puntajeSostenido(),
                propuesta.plazoDiasEstimado(),
                auditoria.getUsuario().getNombre(),
                auditoria.getEstado().tieneTrabajoContratado()
                        ? RespuestasAuditoria.AvancePublico.de(auditoria)
                        : null);
    }

    /**
     * Busca la auditoría verificando que sea del usuario.
     *
     * Si es de otro se devuelve "no encontrada" y no "prohibido": responder
     * distinto le confirmaría a un curioso que ese identificador existe.
     */
    private Auditoria recuperar(Usuario usuario, UUID id) {
        return repositorio.buscarPorId(id)
                .filter(auditoria -> auditoria.perteneceA(usuario))
                .orElseThrow(() -> new AuditoriaNoEncontradaException("No se encontró la auditoría."));
    }

    private RespuestasAuditoria.Informe armarInforme(Auditoria auditoria) {
        return armarInforme(auditoria, null);
    }

    private RespuestasAuditoria.Informe armarInforme(Auditoria auditoria, String avisoDelAnalisis) {
        var respuestas = auditoria.respuestasParaElMotor();
        var resultado = MotorDePuntaje.calcular(respuestas);
        var propuesta = GeneradorDePropuesta.generar(
                respuestas, CatalogoServicios.TODOS, auditoria.getAjustesPropuesta());
        var dictamenes = auditoria.dictamenes();

        List<RespuestasAuditoria.SenalRespondida> senales = List.of(Senal.values()).stream()
                .map(senal -> RespuestasAuditoria.SenalRespondida.de(senal, dictamenes.get(senal)))
                .toList();

        return new RespuestasAuditoria.Informe(
                auditoria.getId(),
                auditoria.getNombre(),
                auditoria.getRubro(),
                auditoria.getCiudad(),
                auditoria.getTelefono(),
                auditoria.getDireccion(),
                auditoria.getSitioWeb(),
                auditoria.getEstado(),
                auditoria.getTokenPublico(),
                auditoria.getCreadaEn(),
                auditoria.getMedicion(),
                resultado.puntajeGlobal(),
                resultado.nivel(),
                resultado.nivel().getLectura(),
                RespuestasAuditoria.aDimensiones(resultado.dimensiones()),
                RespuestasAuditoria.aHallazgos(resultado.planDeAccion()),
                resultado.minutosTotalesEstimados(),
                senales,
                auditoria.paraRevisar().size(),
                auditoria.sinResponder().size(),
                RespuestasAuditoria.calcularCobertura(auditoria.getRespuestas().size()),
                !auditoria.sinResponder().isEmpty(),
                RespuestasAuditoria.Propuesta.de(propuesta),
                // El seguimiento solo existe una vez que el cliente respondió.
                // Devolver un bloque vacío obligaría al frontend a distinguir
                // "sin tareas" de "todavía no se cerró nada", que no es lo mismo.
                auditoria.getEstado().tieneTrabajoContratado()
                        || auditoria.getEstado() == EstadoAuditoria.RECHAZADA
                        ? RespuestasAuditoria.Seguimiento.de(auditoria)
                        : null,
                avisoDelAnalisis);
    }

    public static class SenalNoEditableException extends RuntimeException {
        public SenalNoEditableException(Senal senal) {
            super("La señal " + senal.name() + " la mide el analizador de sitios y no se puede cambiar a mano.");
        }
    }

    public static class SinTrabajoContratadoException extends RuntimeException {
        public SinTrabajoContratadoException() {
            super("Todavía no hay nada contratado en esta auditoría. Marcá la propuesta como aceptada primero.");
        }
    }

    public static class InformeIncompletoException extends RuntimeException {
        public InformeIncompletoException(int pendientes) {
            super("Este informe todavía no está terminado: faltan responder " + pendientes
                    + " señales. Completalo antes de compartir el enlace.");
        }
    }
}
