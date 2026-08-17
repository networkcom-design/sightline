package com.networkcom.lupa.domain.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guarda la medición del sitio como JSON en una sola columna.
 *
 * Se eligió esto en lugar de doce columnas porque la medición es una foto de un
 * momento: se lee entera o no se lee, y nunca se filtra ni se ordena por sus
 * campos. Desarmarla en columnas solo agregaría trabajo de mapeo y migraciones
 * cada vez que el analizador mida una cosa más.
 */
@Converter
public class ConversorMedicionSitio implements AttributeConverter<MedicionSitio, String> {

    private static final Logger log = LoggerFactory.getLogger(ConversorMedicionSitio.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(MedicionSitio medicion) {
        if (medicion == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(medicion);
        } catch (Exception e) {
            log.warn("No se pudo serializar la medición del sitio: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public MedicionSitio convertToEntityAttribute(String columna) {
        if (columna == null || columna.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(columna, MedicionSitio.class);
        } catch (Exception e) {
            // Una medición vieja con otro formato no puede tumbar la auditoría
            // entera: se pierde la foto del sitio, no el diagnóstico.
            log.warn("No se pudo leer la medición guardada: {}", e.getMessage());
            return null;
        }
    }
}
