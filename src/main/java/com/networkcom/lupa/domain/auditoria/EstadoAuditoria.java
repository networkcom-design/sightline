package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * En qué punto del recorrido está una auditoría.
 *
 * Los cuatro primeros estados son el diagnóstico; los tres últimos son lo que
 * pasó después de mandar la propuesta. Que convivan en el mismo enum es a
 * propósito: para la agencia no son dos cosas distintas, es un solo expediente
 * por comercio que arranca en una medición y termina —o no— en un trabajo
 * entregado. Perder de vista cuál de los dos caminos tomó cada propuesta es
 * justamente lo que hace que las ventas se enfríen sin que nadie se entere.
 */
public enum EstadoAuditoria {

    /** Cargado el comercio y medido el sitio. Falta la evidencia. */
    BORRADOR("Borrador"),

    /** Ya se analizó la evidencia. Hay dictámenes esperando revisión. */
    ANALIZADA("Analizada"),

    /** El auditor confirmó las señales. El informe está firme. */
    REVISADA("Revisada"),

    /** Se envió el enlace al prospecto. */
    ENVIADA("Enviada"),

    /** El comercio aceptó el presupuesto. Los precios quedaron congelados. */
    ACEPTADA("Aceptada"),

    /** Hay trabajo empezado sobre lo contratado. */
    EN_EJECUCION("En ejecución"),

    /** Todo lo contratado está entregado o corriendo. */
    ENTREGADA("Entregada"),

    /** El comercio no avanzó. Se guarda el motivo. */
    RECHAZADA("Rechazada");

    private final String etiqueta;

    EstadoAuditoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Si ya hay un trato cerrado y, por lo tanto, tareas que seguir. */
    public boolean tieneTrabajoContratado() {
        return this == ACEPTADA || this == EN_EJECUCION || this == ENTREGADA;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }
}
