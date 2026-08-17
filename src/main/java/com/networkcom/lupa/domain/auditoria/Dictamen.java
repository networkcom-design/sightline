package com.networkcom.lupa.domain.auditoria;

/**
 * La respuesta a una señal, con su procedencia y su fundamento.
 *
 * El fundamento no es decorativo: es lo que permite que el auditor revise en
 * segundos en vez de volver a mirar el perfil. Si la IA dice que la bio no
 * explica qué vende, tiene que citar la bio.
 */
public record Dictamen(
        Senal senal,
        EstadoSenal estado,
        OrigenDeRespuesta origen,
        String fundamento,
        Confianza confianza) {

    public enum Confianza {
        /** La evidencia es explícita y no admite lectura alternativa. */
        ALTA,
        /** Se desprende de la evidencia, pero conviene confirmarlo. */
        MEDIA,
        /** La evidencia es pobre o ambigua. Hay que mirarlo sí o sí. */
        BAJA
    }

    public static Dictamen medido(Senal senal, EstadoSenal estado, String fundamento) {
        return new Dictamen(senal, estado, OrigenDeRespuesta.MEDIDO, fundamento, Confianza.ALTA);
    }

    public static Dictamen deIa(Senal senal, EstadoSenal estado, String fundamento, Confianza confianza) {
        return new Dictamen(senal, estado, OrigenDeRespuesta.IA, fundamento, confianza);
    }

    public static Dictamen humano(Senal senal, EstadoSenal estado) {
        return new Dictamen(senal, estado, OrigenDeRespuesta.HUMANO, null, Confianza.ALTA);
    }

    public static Dictamen pendiente(Senal senal) {
        return new Dictamen(senal, EstadoSenal.NO_APLICA, OrigenDeRespuesta.PENDIENTE, null, Confianza.BAJA);
    }

    /** Lo que hay que revisar sí o sí antes de mandar el informe. */
    public boolean necesitaRevision() {
        return origen == OrigenDeRespuesta.IA && confianza != Confianza.ALTA;
    }
}
