package com.networkcom.lupa.application.auditoria.dto;

import com.networkcom.lupa.domain.auditoria.Auditoria;
import com.networkcom.lupa.domain.auditoria.Dictamen;
import com.networkcom.lupa.domain.auditoria.Dimension;
import com.networkcom.lupa.domain.auditoria.EstadoAuditoria;
import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.MedicionSitio;
import com.networkcom.lupa.domain.auditoria.MotorDePuntaje;
import com.networkcom.lupa.domain.auditoria.NivelPresencia;
import com.networkcom.lupa.domain.auditoria.OrigenDeRespuesta;
import com.networkcom.lupa.domain.auditoria.Senal;
import com.networkcom.lupa.domain.propuesta.EstadoTarea;
import com.networkcom.lupa.domain.propuesta.GeneradorDePropuesta;
import com.networkcom.lupa.domain.propuesta.TareaContratada;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** Todo lo que la API devuelve sobre auditorías. */
public final class RespuestasAuditoria {

    private RespuestasAuditoria() {
    }

    /** Fila del listado. Liviana: no incluye dictámenes ni plan. */
    public record Resumen(
            UUID id,
            String nombre,
            String rubro,
            String ciudad,
            int puntaje,
            NivelPresencia nivel,
            EstadoAuditoria estado,
            int senalesRespondidas,
            int senalesTotales,
            Instant creadaEn,
            /** Cuánto del trabajo contratado está entregado. Cero si no hay trato cerrado. */
            int avance) {

        public static Resumen de(Auditoria auditoria) {
            var resultado = MotorDePuntaje.calcular(auditoria.respuestasParaElMotor());
            return new Resumen(
                    auditoria.getId(),
                    auditoria.getNombre(),
                    auditoria.getRubro(),
                    auditoria.getCiudad(),
                    resultado.puntajeGlobal(),
                    resultado.nivel(),
                    auditoria.getEstado(),
                    auditoria.getRespuestas().size(),
                    Senal.values().length,
                    auditoria.getCreadaEn(),
                    auditoria.avanceDelTrabajo());
        }
    }

    /** Una señal con su respuesta, su origen y su fundamento. */
    public record SenalRespondida(
            String codigo,
            Dimension dimension,
            String pregunta,
            EstadoSenal estado,
            OrigenDeRespuesta origen,
            Dictamen.Confianza confianza,
            String fundamento,
            boolean necesitaRevision,
            boolean editable) {

        public static SenalRespondida de(Senal senal, Dictamen dictamen) {
            return new SenalRespondida(
                    senal.name(),
                    senal.getDimension(),
                    senal.getPregunta(),
                    dictamen == null ? null : dictamen.estado(),
                    dictamen == null ? OrigenDeRespuesta.PENDIENTE : dictamen.origen(),
                    dictamen == null ? null : dictamen.confianza(),
                    dictamen == null ? null : dictamen.fundamento(),
                    dictamen != null && dictamen.necesitaRevision(),
                    // Lo medido no se edita: es un hecho, no una opinión, y dejar
                    // cambiarlo sería fingir que es discutible.
                    !senal.isAutomatica());
        }
    }

    public record PuntajePorDimension(
            Dimension dimension,
            String descripcion,
            int peso,
            int puntaje,
            int senalesCumplidas,
            int senalesEvaluadas) {
    }

    public record Hallazgo(
            String codigo,
            Dimension dimension,
            String titulo,
            String accion,
            String impacto,
            String esfuerzo,
            int minutosEstimados) {

        static Hallazgo de(MotorDePuntaje.Hallazgo origen) {
            return new Hallazgo(
                    origen.codigo(),
                    origen.dimension(),
                    origen.titulo(),
                    origen.accion(),
                    origen.impacto().getEtiqueta(),
                    origen.esfuerzo().getEtiqueta(),
                    origen.minutosEstimados());
        }
    }

    public record ServicioSugerido(
            String codigo,
            String nombre,
            String descripcion,
            long hallazgosQueResuelve,
            BigDecimal precio,
            BigDecimal precioDeLista,
            boolean precioAjustado,
            boolean incluido,
            String modalidad,
            int plazoDias) {
    }

    public record Propuesta(
            List<ServicioSugerido> servicios,
            BigDecimal totalUnico,
            BigDecimal totalMensual,
            int plazoDiasEstimado,
            int puntajeActual,
            int puntajeAlEntregar,
            int puntajeSostenido,
            int mejoraAlEntregar,
            int aporteDelAbono) {

        public static Propuesta de(GeneradorDePropuesta.Propuesta origen) {
            List<ServicioSugerido> servicios = origen.items().stream()
                    .map(item -> new ServicioSugerido(
                            item.servicio().codigo(),
                            item.servicio().nombre(),
                            item.servicio().descripcion(),
                            item.hallazgosQueResuelve(),
                            item.precio(),
                            item.servicio().precioReferenciaArs(),
                            item.precio().compareTo(item.servicio().precioReferenciaArs()) != 0,
                            item.incluido(),
                            item.servicio().modalidad().name(),
                            item.servicio().plazoDias()))
                    .toList();

            return new Propuesta(
                    servicios,
                    origen.totalUnico(),
                    origen.totalMensual(),
                    origen.plazoDiasEstimado(),
                    origen.puntajeActual(),
                    origen.puntajeAlEntregar(),
                    origen.puntajeSostenido(),
                    origen.mejoraAlEntregar(),
                    origen.loQueAportaElAbono());
        }
    }

    /** Una tarea contratada, como la ve el auditor. */
    public record TareaEnCurso(
            String codigoServicio,
            String nombre,
            BigDecimal precio,
            String modalidad,
            int plazoDias,
            EstadoTarea estado,
            String estadoEtiqueta,
            /** Los tres estados con el nombre que les corresponde a esta modalidad. */
            List<OpcionDeEstado> opciones,
            boolean cumplida,
            Instant iniciadaEn,
            Instant completadaEn,
            String nota) {

        static TareaEnCurso de(TareaContratada tarea) {
            List<OpcionDeEstado> opciones = Arrays.stream(EstadoTarea.values())
                    .map(estado -> new OpcionDeEstado(
                            estado, TareaContratada.etiquetaDe(estado, tarea.getModalidad())))
                    .toList();

            return new TareaEnCurso(
                    tarea.getCodigoServicio(),
                    tarea.getNombre(),
                    tarea.getPrecio(),
                    tarea.getModalidad().name(),
                    tarea.getPlazoDias(),
                    tarea.getEstado(),
                    tarea.etiquetaDeEstado(),
                    opciones,
                    tarea.estaCumplida(),
                    tarea.getIniciadaEn(),
                    tarea.getCompletadaEn(),
                    tarea.getNota());
        }
    }

    public record OpcionDeEstado(EstadoTarea estado, String etiqueta) {
    }

    /**
     * El estado del trabajo después de que el cliente respondió.
     *
     * Los totales salen de las tareas y no del generador de propuestas: son los
     * precios congelados el día que se aceptó, que es lo que hay que cobrar.
     */
    public record Seguimiento(
            List<TareaEnCurso> tareas,
            int avance,
            BigDecimal totalUnicoContratado,
            BigDecimal totalMensualContratado,
            Instant aceptadaEn,
            Instant rechazadaEn,
            String motivoRechazo,
            Instant entregadaEn) {

        public static Seguimiento de(Auditoria auditoria) {
            return new Seguimiento(
                    auditoria.getTareas().stream().map(TareaEnCurso::de).toList(),
                    auditoria.avanceDelTrabajo(),
                    auditoria.totalContratado(false),
                    auditoria.totalContratado(true),
                    auditoria.getAceptadaEn(),
                    auditoria.getRechazadaEn(),
                    auditoria.getMotivoRechazo(),
                    auditoria.getEntregadaEn());
        }
    }

    /** El informe interno completo. Es lo que ve el auditor, con todo el detalle. */
    public record Informe(
            UUID id,
            String nombre,
            String rubro,
            String ciudad,
            String telefono,
            String direccion,
            String sitioWeb,
            EstadoAuditoria estado,
            String tokenPublico,
            Instant creadaEn,
            MedicionSitio medicion,
            int puntaje,
            NivelPresencia nivel,
            String lecturaDelNivel,
            List<PuntajePorDimension> dimensiones,
            List<Hallazgo> planDeAccion,
            int minutosTotalesEstimados,
            List<SenalRespondida> senales,
            int senalesParaRevisar,
            int senalesPendientes,
            int cobertura,
            boolean provisional,
            Propuesta propuesta,
            /** Lo contratado y su avance. Nulo mientras el cliente no respondió. */
            Seguimiento seguimiento,
            /** Por qué el análisis no completó todo. Nulo si salió bien. */
            String avisoDelAnalisis) {
    }

    /**
     * Qué porcentaje de las señales tiene respuesta.
     *
     * Hace falta porque el motor solo puntúa lo que se le contestó: una
     * auditoría con nueve señales medidas y treinta y cuatro sin tocar daría un
     * puntaje altísimo, no porque el comercio esté bien sino porque casi nada se
     * miró todavía. Mientras la cobertura no sea completa, el puntaje es
     * provisional y hay que decirlo.
     */
    public static int calcularCobertura(int respondidas) {
        return Math.round(100f * respondidas / Senal.values().length);
    }

    /**
     * La vista del prospecto.
     *
     * Es un tipo aparte y no un informe recortado a propósito: si fuera el mismo
     * objeto con campos ocultados, cualquier descuido futuro filtraría precios y
     * horas internas. Acá esos campos directamente no existen.
     */
    public record VistaPublica(
            String nombre,
            String rubro,
            String ciudad,
            Instant fecha,
            int puntaje,
            NivelPresencia nivel,
            String lecturaDelNivel,
            List<PuntajePorDimension> dimensiones,
            List<PrioridadPublica> prioridades,
            int puntajeAlEntregar,
            int puntajeSostenido,
            int plazoDiasEstimado,
            String auditadoPor,
            /** El avance del trabajo. Nulo mientras no haya nada contratado. */
            AvancePublico avance) {
    }

    /**
     * Lo que el comercio ve de su propio trabajo en marcha.
     *
     * Sin precios y sin fechas de inicio: al cliente le importa qué se está
     * haciendo y cuánto falta, no cuánto sale cada parte —eso ya lo acordó— ni
     * qué día se tocó cada botón.
     */
    public record AvancePublico(
            List<String> entregado,
            List<String> enCurso,
            List<String> pendiente,
            int avance,
            Instant desde) {

        public static AvancePublico de(Auditoria auditoria) {
            List<TareaContratada> tareas = auditoria.getTareas();
            return new AvancePublico(
                    nombresDe(tareas, TareaContratada::estaCumplida),
                    nombresDe(tareas, tarea -> !tarea.estaCumplida()
                            && tarea.getEstado() != EstadoTarea.PENDIENTE),
                    nombresDe(tareas, tarea -> tarea.getEstado() == EstadoTarea.PENDIENTE),
                    auditoria.avanceDelTrabajo(),
                    auditoria.getAceptadaEn());
        }

        private static List<String> nombresDe(List<TareaContratada> tareas,
                                              Predicate<TareaContratada> filtro) {
            return tareas.stream().filter(filtro).map(TareaContratada::getNombre).toList();
        }
    }

    /**
     * El mensaje listo para mandar, con los enlaces ya armados.
     *
     * `enlaceWhatsApp` y `enlaceCorreo` vienen del backend y no se arman en el
     * navegador porque la normalización del teléfono argentino vive acá, con sus
     * pruebas: el frontend solo abre lo que recibe.
     */
    public record MensajeParaEnviar(
            String canal,
            String asunto,
            String cuerpo,
            String enlacePublico,
            String telefonoNormalizado,
            String telefonoParaMostrar,
            boolean telefonoValido,
            String enlaceWhatsApp,
            boolean redactadoPorIa) {
    }

    /** Un hallazgo contado para el dueño del comercio, sin horas ni códigos internos. */
    public record PrioridadPublica(String titulo, String queHacer, String impacto) {

        static PrioridadPublica de(MotorDePuntaje.Hallazgo origen) {
            return new PrioridadPublica(origen.titulo(), origen.accion(), origen.impacto().getEtiqueta());
        }
    }

    public static List<Hallazgo> aHallazgos(List<MotorDePuntaje.Hallazgo> origen) {
        return origen.stream().map(Hallazgo::de).toList();
    }

    public static List<PrioridadPublica> aPrioridades(List<MotorDePuntaje.Hallazgo> origen, int cuantas) {
        return origen.stream().limit(cuantas).map(PrioridadPublica::de).toList();
    }

    public static List<PuntajePorDimension> aDimensiones(List<MotorDePuntaje.PuntajeDimension> origen) {
        return origen.stream()
                .map(puntaje -> new PuntajePorDimension(
                        puntaje.dimension(),
                        puntaje.dimension().getDescripcion(),
                        puntaje.dimension().getPeso(),
                        puntaje.puntaje(),
                        puntaje.senalesCumplidas(),
                        puntaje.senalesEvaluadas()))
                .toList();
    }
}
