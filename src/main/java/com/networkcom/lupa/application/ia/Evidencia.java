package com.networkcom.lupa.application.ia;

import com.networkcom.lupa.domain.auditoria.MedicionSitio;

/**
 * Todo lo que se sabe del comercio antes de analizar.
 *
 * Los textos de Instagram y Google los pega el auditor mirando la pantalla. No
 * se leen automáticamente a propósito: Instagram bloquea lectores automáticos y
 * la API de Google es paga. Pegar y revisar tarda menos que contestar treinta
 * preguntas, y no se rompe nunca.
 */
public record Evidencia(
        String nombreDeclarado,
        String rubro,
        String ciudad,
        String telefonoDeclarado,
        String direccionDeclarada,
        String sitioWeb,
        MedicionSitio medicionSitio,
        String textoInstagram,
        String textoFichaGoogle,
        String notasDelAuditor) {

    public boolean tieneInstagram() {
        return textoInstagram != null && !textoInstagram.isBlank();
    }

    public boolean tieneFichaGoogle() {
        return textoFichaGoogle != null && !textoFichaGoogle.isBlank();
    }

    public boolean tieneSitioMedido() {
        return medicionSitio != null && medicionSitio.alcanzable();
    }
}
