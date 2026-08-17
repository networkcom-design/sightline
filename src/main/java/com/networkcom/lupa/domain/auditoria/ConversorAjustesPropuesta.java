package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networkcom.lupa.domain.propuesta.AjusteServicio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/** Guarda los ajustes del presupuesto como JSON en una sola columna. */
@Converter
public class ConversorAjustesPropuesta
        implements AttributeConverter<Map<String, AjusteServicio>, String> {

    private static final Logger log = LoggerFactory.getLogger(ConversorAjustesPropuesta.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, AjusteServicio>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, AjusteServicio> ajustes) {
        if (ajustes == null || ajustes.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(ajustes);
        } catch (Exception e) {
            log.warn("No se pudieron serializar los ajustes de la propuesta: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, AjusteServicio> convertToEntityAttribute(String columna) {
        if (columna == null || columna.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON.readValue(columna, TIPO);
        } catch (Exception e) {
            // Perder los ajustes vuelve a la propuesta por defecto, que es
            // recuperable. Tumbar la auditoria entera por un JSON viejo, no.
            log.warn("No se pudieron leer los ajustes guardados: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }
}
