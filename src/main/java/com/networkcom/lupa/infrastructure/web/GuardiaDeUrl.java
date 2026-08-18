package com.networkcom.lupa.infrastructure.web;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Protege al servidor de que le hagan pedir direcciones que no debería.
 *
 * Sightline baja el sitio que le indica el usuario, y eso es una puerta abierta:
 * alguien podría pasar `http://localhost:8080/actuator` o una IP interna de la
 * red y usar el servidor como intermediario para alcanzar cosas que desde
 * afuera no se ven. Es el ataque conocido como SSRF.
 *
 * La defensa es resolver el nombre a IP antes de conectar y rechazar todo lo
 * que apunte a la propia máquina o a rangos privados.
 */
public final class GuardiaDeUrl {

    private static final Set<String> ESQUEMAS_PERMITIDOS = Set.of("http", "https");

    private GuardiaDeUrl() {
    }

    public static final class UrlNoPermitidaException extends RuntimeException {
        public UrlNoPermitidaException(String mensaje) {
            super(mensaje);
        }
    }

    /**
     * Normaliza y valida la dirección. Devuelve la URI lista para usar.
     * Si el usuario escribió el dominio pelado, se le antepone https.
     */
    public static URI validar(String direccionCruda) {
        String direccion = direccionCruda.trim();

        if (direccion.isEmpty()) {
            throw new UrlNoPermitidaException("La dirección del sitio está vacía.");
        }

        if (!direccion.matches("(?i)^https?://.*")) {
            direccion = "https://" + direccion;
        }

        URI uri;
        try {
            uri = URI.create(direccion);
        } catch (IllegalArgumentException e) {
            throw new UrlNoPermitidaException("La dirección no tiene un formato válido.");
        }

        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!ESQUEMAS_PERMITIDOS.contains(esquema)) {
            throw new UrlNoPermitidaException("Solo se pueden analizar direcciones http o https.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UrlNoPermitidaException("La dirección no tiene un dominio válido.");
        }

        verificarQueSeaPublica(host);
        return uri;
    }

    private static void verificarQueSeaPublica(String host) {
        InetAddress[] direcciones;
        try {
            direcciones = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new UrlNoPermitidaException("No se pudo resolver el dominio " + host + ".");
        }

        for (InetAddress direccion : direcciones) {
            if (direccion.isLoopbackAddress()
                    || direccion.isSiteLocalAddress()
                    || direccion.isLinkLocalAddress()
                    || direccion.isAnyLocalAddress()
                    || direccion.isMulticastAddress()) {
                throw new UrlNoPermitidaException(
                        "Solo se pueden analizar sitios públicos, no direcciones internas de la red.");
            }
        }
    }
}
