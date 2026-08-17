package com.networkcom.lupa.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuracion propia de la aplicacion, tipada.
 *
 * Tenerla como record en lugar de @Value sueltos hace que Spring falle al
 * arrancar si falta algo, en vez de descubrirlo cuando alguien intenta loguearse.
 */
@ConfigurationProperties(prefix = "lupa")
public record PropiedadesLupa(Jwt jwt, Cors cors) {

    public record Jwt(String secreto, long duracionHoras) {
    }

    public record Cors(List<String> origenes) {
    }
}
