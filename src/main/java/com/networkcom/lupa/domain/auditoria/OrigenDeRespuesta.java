package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * De dónde salió la respuesta de una señal.
 *
 * Se guarda porque cambia cuánto hay que confiar en ella. Un dato medido no se
 * discute; un dictamen de la IA se revisa; una respuesta humana es la palabra
 * final. En la pantalla de revisión, esto es lo que decide qué mirar primero.
 */
public enum OrigenDeRespuesta {

    /** Medido por el analizador de sitios. Es un hecho, no una opinión. */
    MEDIDO("Medido"),

    /** Dictaminado por la IA a partir de la evidencia pegada. Se revisa. */
    IA("Analizado por IA"),

    /** Contestado por el auditor. Pisa cualquier otro origen. */
    HUMANO("Respondido por vos"),

    /** Todavía sin responder. */
    PENDIENTE("Pendiente");

    private final String etiqueta;

    OrigenDeRespuesta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }
}
