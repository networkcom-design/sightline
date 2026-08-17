package com.networkcom.lupa.infrastructure.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networkcom.lupa.application.ia.ProveedorIA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ConfiguracionIA {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionIA.class);

    /**
     * El proveedor real, solo si hay clave configurada.
     *
     * Se decide con una condición en código y no con `@ConditionalOnProperty`
     * porque la clave puede venir definida pero vacía, que es el caso más común
     * cuando alguien clona el proyecto sin configurar nada.
     */
    @Bean
    public ProveedorIA proveedorIA(RestClient.Builder constructor, ObjectMapper json, PropiedadesIA propiedades) {
        if (!propiedades.estaConfigurada()) {
            log.warn("Sin clave de IA configurada. Las señales que iba a dictaminar la IA "
                    + "van a quedar para el cuestionario manual. Definí LUPA_GEMINI_API_KEY para activarla.");
            return new ProveedorSinConfigurar();
        }

        log.info("Proveedor de IA activo: Gemini {}", propiedades.modelo());
        return new ProveedorGemini(constructor, json, propiedades);
    }

    /**
     * Sustituto para cuando no hay clave.
     *
     * Falla siempre, y eso es deliberado: el analista atrapa la falla y deja las
     * señales sin responder, que caen al cuestionario. La alternativa —devolver
     * una respuesta vacía que parezca válida— haría que el informe afirme cosas
     * que nadie verificó.
     */
    static class ProveedorSinConfigurar implements ProveedorIA {

        @Override
        public String responder(String instrucciones, String contenido) {
            throw new ProveedorIAException("No hay proveedor de IA configurado.");
        }

        @Override
        public String nombre() {
            return "sin configurar";
        }
    }
}
