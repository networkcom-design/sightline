package com.networkcom.lupa.infrastructure.web;

import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.MedicionSitio;
import com.networkcom.lupa.domain.auditoria.Senal;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Analiza de verdad el sitio del comercio: lo baja, lo mide y lo revisa.
 *
 * Esta es la única parte de Lupa que no necesita que nadie conteste nada. Todo
 * lo que devuelve son hechos medidos, no percepciones.
 */
@Component
public class AnalizadorSitioWeb {

    private static final Logger log = LoggerFactory.getLogger(AnalizadorSitioWeb.class);

    private static final int TIEMPO_MAXIMO_MS = 12_000;
    private static final int PESO_MAXIMO_BYTES = 5 * 1024 * 1024;
    private static final int VELOCIDAD_ACEPTABLE_MS = 2_500;

    private static final int TITULO_MINIMO = 20;
    private static final int TITULO_MAXIMO = 70;
    private static final int META_MINIMO = 50;

    /** Un navegador declarado de verdad: muchos hostings rechazan agentes vacíos. */
    private static final String AGENTE =
            "Mozilla/5.0 (compatible; LupaBot/1.0; +https://networkcom.com.ar/lupa)";

    public record Resultado(MedicionSitio medicion, Map<Senal, EstadoSenal> senales) {
    }

    /**
     * Analiza el sitio. Si el comercio no tiene, se pasa `null` o vacío y todas
     * las señales del sitio quedan en incumplidas: no tener sitio es un
     * hallazgo, no un "no aplica".
     */
    public Resultado analizar(String urlCruda) {
        Map<Senal, EstadoSenal> senales = new EnumMap<>(Senal.class);

        if (urlCruda == null || urlCruda.isBlank()) {
            Senal.automaticas().forEach(senal -> senales.put(senal, EstadoSenal.NO_CUMPLE));
            return new Resultado(MedicionSitio.fallida(null, "El comercio no tiene sitio web."), senales);
        }

        URI uri;
        try {
            uri = GuardiaDeUrl.validar(urlCruda);
        } catch (GuardiaDeUrl.UrlNoPermitidaException e) {
            Senal.automaticas().forEach(senal -> senales.put(senal, EstadoSenal.NO_CUMPLE));
            return new Resultado(MedicionSitio.fallida(urlCruda, e.getMessage()), senales);
        }

        // El sitio existe como dato declarado, más allá de si después responde.
        senales.put(Senal.WEB_EXISTE, EstadoSenal.CUMPLE);

        long comienzo = System.nanoTime();

        try {
            Connection.Response respuesta = Jsoup.connect(uri.toString())
                    .userAgent(AGENTE)
                    .timeout(TIEMPO_MAXIMO_MS)
                    .maxBodySize(PESO_MAXIMO_BYTES)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();

            long milisegundos = (System.nanoTime() - comienzo) / 1_000_000;

            boolean ok = respuesta.statusCode() >= 200 && respuesta.statusCode() < 400;
            senales.put(Senal.WEB_RESPONDE, ok ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);

            if (!ok) {
                return new Resultado(
                        new MedicionSitio(respuesta.url().toString(), false, respuesta.statusCode(), milisegundos,
                                0, null, null, 0, false, esHttps(respuesta.url().toString()), false,
                                "El sitio respondió con código " + respuesta.statusCode() + "."),
                        completarFaltantes(senales));
            }

            // El orden importa: jsoup consume el flujo del cuerpo al parsear, así
            // que hay que leerlo como texto antes y recién después construir el
            // documento a partir de esa cadena.
            String cuerpo = respuesta.body();
            int pesoKb = cuerpo.getBytes(StandardCharsets.UTF_8).length / 1024;
            Document documento = Jsoup.parse(cuerpo, respuesta.url().toString());

            return new Resultado(
                    medir(respuesta.url().toString(), respuesta.statusCode(), milisegundos, pesoKb, documento, senales),
                    completarFaltantes(senales));

        } catch (IOException e) {
            log.info("No se pudo analizar {}: {}", uri, e.getMessage());
            senales.put(Senal.WEB_RESPONDE, EstadoSenal.NO_CUMPLE);
            return new Resultado(
                    MedicionSitio.fallida(uri.toString(), "No se pudo conectar con el sitio."),
                    completarFaltantes(senales));
        }
    }

    private MedicionSitio medir(String urlFinal, int codigo, long milisegundos, int pesoKb,
                           Document documento, Map<Senal, EstadoSenal> senales) {

        String titulo = documento.title().trim();
        String meta = documento.select("meta[name=description]").attr("content").trim();
        int cantidadH1 = documento.select("h1").size();
        boolean viewport = !documento.select("meta[name=viewport]").isEmpty();
        boolean https = esHttps(urlFinal);
        boolean contacto = tieneContactoVisible(documento);

        senales.put(Senal.WEB_HTTPS, https ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);
        senales.put(Senal.WEB_MOBILE, viewport ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);
        senales.put(Senal.WEB_H1, cantidadH1 == 1 ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);
        senales.put(Senal.WEB_CONTACTO, contacto ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);

        boolean tituloOk = titulo.length() >= TITULO_MINIMO && titulo.length() <= TITULO_MAXIMO;
        senales.put(Senal.WEB_TITULO, tituloOk ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);

        senales.put(Senal.WEB_META, meta.length() >= META_MINIMO ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);

        senales.put(Senal.WEB_VELOCIDAD,
                milisegundos <= VELOCIDAD_ACEPTABLE_MS ? EstadoSenal.CUMPLE : EstadoSenal.NO_CUMPLE);

        return new MedicionSitio(urlFinal, true, codigo, milisegundos, pesoKb, titulo,
                meta.isBlank() ? null : meta, cantidadH1, viewport, https, contacto, null);
    }

    /**
     * Busca un canal de contacto real, no la palabra "contacto".
     * Un enlace `tel:`, `wa.me` o `mailto:` es contacto; un menú que dice
     * "Contactanos" y lleva a un formulario enterrado, no.
     */
    private boolean tieneContactoVisible(Document documento) {
        return !documento.select("a[href^=tel:]").isEmpty()
                || !documento.select("a[href^=mailto:]").isEmpty()
                || !documento.select("a[href*=wa.me]").isEmpty()
                || !documento.select("a[href*=api.whatsapp.com]").isEmpty();
    }

    private boolean esHttps(String url) {
        return url != null && url.toLowerCase().startsWith("https://");
    }

    /** Lo que no se pudo medir cuenta como incumplido, no como inexistente. */
    private Map<Senal, EstadoSenal> completarFaltantes(Map<Senal, EstadoSenal> senales) {
        Senal.automaticas().forEach(senal -> senales.putIfAbsent(senal, EstadoSenal.NO_CUMPLE));
        return senales;
    }
}
