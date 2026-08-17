package com.networkcom.lupa.domain.auditoria;

/**
 * Lo que se pudo medir del sitio del comercio.
 *
 * Vive en el dominio y no en infraestructura porque la auditoría la guarda como
 * parte de su estado: si mañana cambiamos la forma de bajar sitios, esto no se
 * tiene que enterar.
 */
public record MedicionSitio(
        String urlFinal,
        boolean alcanzable,
        int codigoHttp,
        long milisegundos,
        int pesoKb,
        String titulo,
        String metaDescripcion,
        int cantidadH1,
        boolean tieneViewport,
        boolean esHttps,
        boolean tieneContactoVisible,
        String error) {

    public static MedicionSitio fallida(String url, String error) {
        return new MedicionSitio(url, false, 0, 0, 0, null, null, 0, false, false, false, error);
    }

    public static MedicionSitio sinSitio() {
        return fallida(null, "El comercio no tiene sitio web.");
    }

    /** Segundos con un decimal, para mostrarlo en el informe. */
    public double segundos() {
        return Math.round(milisegundos / 100.0) / 10.0;
    }
}
