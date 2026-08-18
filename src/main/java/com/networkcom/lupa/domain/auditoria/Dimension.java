package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Las siete dimensiones que mide Sightline.
 *
 * El peso define cuanto influye cada una en el puntaje global. No son iguales
 * a proposito: la ficha de Google es donde un comercio local pierde clientes
 * todos los dias, mientras que Facebook hoy casi no mueve la aguja. Los pesos
 * suman 100.
 */
public enum Dimension {

    GOOGLE("Ficha de Google", 25,
            "Como aparece el comercio cuando alguien lo busca en Google o Maps."),

    INSTAGRAM("Instagram", 20,
            "Si el perfil explica que vende, como contactar y si publica seguido."),

    SITIO_WEB("Sitio web", 15,
            "Si existe, si carga rapido, si se ve en el celular y si los buscadores lo entienden."),

    REPUTACION("Reputacion", 15,
            "Volumen, promedio y manejo de las resenas."),

    WHATSAPP("WhatsApp Business", 10,
            "Si el canal por el que realmente compra la gente esta preparado para vender."),

    CONSISTENCIA("Consistencia de datos", 8,
            "Que el nombre, la direccion y el telefono sean identicos en todos lados."),

    CONTENIDO("Contenido", 7,
            "Frecuencia, variedad y claridad de lo que publica.");

    private final String etiqueta;
    private final int peso;
    private final String descripcion;

    Dimension(String etiqueta, int peso, String descripcion) {
        this.etiqueta = etiqueta;
        this.peso = peso;
        this.descripcion = descripcion;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    public int getPeso() {
        return peso;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Verifica que los pesos sumen 100. Se llama al arrancar la aplicacion: si
     * alguien agrega una dimension y se olvida de rebalancear, es mejor que la
     * app no levante a que emita puntajes en una escala equivocada.
     */
    public static void validarPesos() {
        int total = 0;
        for (Dimension dimension : values()) {
            total += dimension.peso;
        }
        if (total != 100) {
            throw new IllegalStateException(
                    "Los pesos de las dimensiones tienen que sumar 100 y suman " + total + ".");
        }
    }
}
