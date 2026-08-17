package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonValue;

/** En qué punto del recorrido está una auditoría. */
public enum EstadoAuditoria {

    /** Cargado el comercio y medido el sitio. Falta la evidencia. */
    BORRADOR("Borrador"),

    /** Ya se analizó la evidencia. Hay dictámenes esperando revisión. */
    ANALIZADA("Analizada"),

    /** El auditor confirmó las señales. El informe está firme. */
    REVISADA("Revisada"),

    /** Se envió el enlace al prospecto. */
    ENVIADA("Enviada");

    private final String etiqueta;

    EstadoAuditoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }
}
