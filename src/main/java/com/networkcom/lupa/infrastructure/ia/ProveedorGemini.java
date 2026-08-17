package com.networkcom.lupa.infrastructure.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networkcom.lupa.application.ia.ProveedorIA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador para la API de Gemini.
 *
 * Le pide al modelo que responda en JSON usando `responseMimeType`, que es la
 * forma soportada de pedir salida estructurada en lugar de rogarlo en el prompt.
 * Aun así, el analista tolera que venga envuelto en texto: la instrucción reduce
 * el problema, no lo elimina.
 */
public class ProveedorGemini implements ProveedorIA {

    private static final Logger log = LoggerFactory.getLogger(ProveedorGemini.class);

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient cliente;
    private final ObjectMapper json;
    private final PropiedadesIA propiedades;

    public ProveedorGemini(RestClient.Builder constructor, ObjectMapper json, PropiedadesIA propiedades) {
        this.json = json;
        this.propiedades = propiedades;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        fabrica.setReadTimeout((int) Duration.ofSeconds(propiedades.tiempoLimiteSegundos()).toMillis());

        // El tiempo límite importa más de lo que parece: sin él, una consulta
        // colgada deja al auditor esperando frente a una pantalla vacía en vez
        // de caer al cuestionario, que es la salida prevista.
        this.cliente = constructor.baseUrl(BASE).requestFactory(fabrica).build();
    }

    @Override
    public String responder(String instrucciones, String contenido) {
        return consultar(instrucciones, contenido, false);
    }

    @Override
    public String responderJson(String instrucciones, String contenido) {
        return consultar(instrucciones, contenido, true);
    }

    private String consultar(String instrucciones, String contenido, boolean esperaJson) {
        // La temperatura sube un poco para redactar: en los dictámenes se busca
        // que el mismo perfil dé siempre la misma respuesta, pero en un mensaje
        // de venta esa repetición se nota y suena a plantilla.
        Map<String, Object> generacion = esperaJson
                ? Map.of("temperature", 0.2, "responseMimeType", "application/json")
                : Map.of("temperature", 0.7);

        Map<String, Object> cuerpo = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", instrucciones))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", contenido)))),
                "generationConfig", generacion);

        try {
            String respuesta = cliente.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(propiedades.modelo() + ":generateContent")
                            .queryParam("key", propiedades.apiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);

            return extraerTexto(respuesta);

        } catch (ProveedorIAException e) {
            throw e;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new LimiteAlcanzadoException(segundosDeEspera(e.getResponseBodyAsString()));
        } catch (Exception e) {
            throw new ProveedorIAException("No se pudo consultar a Gemini: " + e.getMessage(), e);
        }
    }

    /**
     * Se alcanzó el límite de consultas de la cuenta.
     *
     * Tiene su propio tipo porque no es un error cualquiera: hay que decírselo
     * al usuario con esas palabras. Si se muestra como "falló el análisis", el
     * auditor va a reintentar una y otra vez sin entender que el problema es la
     * cuota y que hay que esperar.
     */
    public static class LimiteAlcanzadoException extends ProveedorIAException {
        private final int segundosDeEspera;

        public LimiteAlcanzadoException(int segundosDeEspera) {
            super("Se alcanzó el límite de consultas del plan de Gemini."
                    + (segundosDeEspera > 0 ? " Reintentá en " + segundosDeEspera + " segundos." : ""));
            this.segundosDeEspera = segundosDeEspera;
        }

        public int getSegundosDeEspera() {
            return segundosDeEspera;
        }
    }

    /** Google indica cuánto falta en `retryDelay`. Si no viene, se devuelve 0. */
    private int segundosDeEspera(String cuerpoDelError) {
        Matcher coincidencia = Pattern.compile("\"retryDelay\"\\s*:\\s*\"(\\d+)s\"").matcher(cuerpoDelError);
        return coincidencia.find() ? Integer.parseInt(coincidencia.group(1)) : 0;
    }

    private String extraerTexto(String respuestaCruda) {
        try {
            JsonNode raiz = json.readTree(respuestaCruda);
            JsonNode texto = raiz.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (texto.isMissingNode() || texto.asText().isBlank()) {
                String motivo = raiz.path("candidates").path(0).path("finishReason").asText("desconocido");
                log.warn("Gemini no devolvió texto. Motivo declarado: {}", motivo);
                throw new ProveedorIAException("Gemini respondió sin contenido (" + motivo + ").");
            }

            return texto.asText();

        } catch (ProveedorIAException e) {
            throw e;
        } catch (Exception e) {
            throw new ProveedorIAException("La respuesta de Gemini no se pudo interpretar.", e);
        }
    }

    @Override
    public String nombre() {
        return "Gemini " + propiedades.modelo();
    }
}
