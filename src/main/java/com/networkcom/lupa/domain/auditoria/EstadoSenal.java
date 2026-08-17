package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * En qué estado quedó una señal al evaluarla.
 *
 * `NO_APLICA` no es lo mismo que `NO_CUMPLE`: a un puesto de feria sin local
 * fijo no se le puede reclamar la dirección en Google. Las que no aplican salen
 * del cálculo en lugar de restar, para que el puntaje no castigue al comercio
 * por algo que no le corresponde.
 */
public enum EstadoSenal {

    CUMPLE("Cumple"),
    NO_CUMPLE("No cumple"),
    NO_APLICA("No aplica");

    private final String etiqueta;

    EstadoSenal(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Acepta tanto el código como la etiqueta.
     *
     * Hace falta porque `@JsonValue` cambia la salida a "No cumple", y sin esto
     * Jackson exigiría exactamente ese texto al recibir, rechazando el
     * `NO_CUMPLE` que es lo natural mandar desde un cliente.
     */
    @JsonCreator
    public static EstadoSenal desde(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Falta el estado de la señal.");
        }
        String limpio = valor.strip();

        for (EstadoSenal estado : values()) {
            if (estado.name().equalsIgnoreCase(limpio) || estado.etiqueta.equalsIgnoreCase(limpio)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado desconocido: " + valor);
    }
}
