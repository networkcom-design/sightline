package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * La lectura en castellano del puntaje global.
 *
 * Existe porque un número suelto no le dice nada a quien recibe el informe:
 * "62" no significa nada, "competitivo pero con huecos" sí.
 */
public enum NivelPresencia {

    CRITICO("Crítico", 0, 39,
            "El negocio es prácticamente invisible online. Hay clientes buscándolo que no lo encuentran."),

    BASICO("Básico", 40, 59,
            "Está presente pero desaprovechado. Lo esencial existe y funciona a medias."),

    COMPETITIVO("Competitivo", 60, 79,
            "Compite bien en su zona. Le faltan detalles que hoy lo separan de los primeros puestos."),

    REFERENTE("Referente", 80, 100,
            "Está entre los mejores de su rubro en la zona. Ahora se trata de sostener y medir.");

    private final String etiqueta;
    private final int desde;
    private final int hasta;
    private final String lectura;

    NivelPresencia(String etiqueta, int desde, int hasta, String lectura) {
        this.etiqueta = etiqueta;
        this.desde = desde;
        this.hasta = hasta;
        this.lectura = lectura;
    }

    public static NivelPresencia desdePuntaje(int puntaje) {
        for (NivelPresencia nivel : values()) {
            if (puntaje >= nivel.desde && puntaje <= nivel.hasta) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Puntaje fuera de la escala 0 a 100: " + puntaje);
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    public String getLectura() {
        return lectura;
    }

    public int getDesde() {
        return desde;
    }

    public int getHasta() {
        return hasta;
    }
}
