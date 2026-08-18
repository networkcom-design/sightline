package com.networkcom.lupa.infrastructure.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del proveedor de IA.
 *
 * La clave se deja vacía por defecto para que el proyecto arranque recién
 * clonado. Sin clave, Sightline funciona igual: las señales que iba a dictaminar la
 * IA caen al cuestionario manual.
 */
@ConfigurationProperties(prefix = "lupa.ia")
public record PropiedadesIA(String apiKey, String modelo, int tiempoLimiteSegundos) {

    public boolean estaConfigurada() {
        return apiKey != null && !apiKey.isBlank();
    }
}
