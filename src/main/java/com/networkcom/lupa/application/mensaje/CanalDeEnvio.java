package com.networkcom.lupa.application.mensaje;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CanalDeEnvio {

    WHATSAPP("WhatsApp"),
    EMAIL("Email");

    private final String etiqueta;

    CanalDeEnvio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    @JsonCreator
    public static CanalDeEnvio desde(String valor) {
        for (CanalDeEnvio canal : values()) {
            if (canal.name().equalsIgnoreCase(valor) || canal.etiqueta.equalsIgnoreCase(valor)) {
                return canal;
            }
        }
        throw new IllegalArgumentException("Canal desconocido: " + valor);
    }
}
