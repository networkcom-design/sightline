package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonValue;

/** Cuánto cuesta resolver un hallazgo. */
public enum Esfuerzo {

    BAJO("Bajo", 1),
    MEDIO("Medio", 2),
    ALTO("Alto", 3);

    private final String etiqueta;
    private final int valor;

    Esfuerzo(String etiqueta, int valor) {
        this.etiqueta = etiqueta;
        this.valor = valor;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    public int getValor() {
        return valor;
    }
}
