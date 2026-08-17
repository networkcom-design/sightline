package com.networkcom.lupa.application.auditoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del comercio para arrancar una auditoría.
 *
 * Solo el nombre y el rubro son obligatorios: el resto puede faltar, y de hecho
 * que falte es información. Un comercio sin sitio web declarado no es un
 * formulario incompleto, es un hallazgo.
 */
public record PeticionNuevaAuditoria(

        @NotBlank(message = "El nombre del comercio no puede estar vacío.")
        @Size(max = 160, message = "El nombre no puede superar los 160 caracteres.")
        String nombre,

        @NotBlank(message = "El rubro no puede estar vacío.")
        @Size(max = 120, message = "El rubro no puede superar los 120 caracteres.")
        String rubro,

        @Size(max = 120) String ciudad,
        @Size(max = 60) String telefono,
        @Size(max = 200) String direccion,
        @Size(max = 300) String sitioWeb) {
}
