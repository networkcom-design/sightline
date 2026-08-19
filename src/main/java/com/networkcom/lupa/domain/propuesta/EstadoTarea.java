package com.networkcom.lupa.domain.propuesta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * En qué punto está un servicio ya contratado.
 *
 * Las etiquetas dependen de la modalidad y por eso no viven acá: para un
 * servicio de pago único "En curso" significa que se está haciendo y
 * "Completado" que se entregó, mientras que para un abono mensual "En curso"
 * es el estado normal —el trabajo está corriendo— y "Completado" quiere decir
 * que se dio de baja. La traducción la hace {@link TareaContratada}.
 */
public enum EstadoTarea {

    PENDIENTE,
    EN_CURSO,
    COMPLETADA;

    @JsonValue
    public String getCodigo() {
        return name();
    }

    @JsonCreator
    public static EstadoTarea desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta el estado de la tarea.");
        }
        return valueOf(valor.trim().toUpperCase());
    }
}
