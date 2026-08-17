package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonValue;

/** Cuánto mueve la aguja resolver un hallazgo. */
public enum Impacto {

    ALTO("Alto", 3),
    MEDIO("Medio", 2),
    BAJO("Bajo", 1);

    private final String etiqueta;
    private final int valor;

    Impacto(String etiqueta, int valor) {
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
