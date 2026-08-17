package com.networkcom.lupa.application.auditoria.dto;

import jakarta.validation.constraints.Size;

/**
 * Los textos que el auditor pega mirando cada perfil.
 *
 * El límite de 20.000 caracteres por campo no es capricho: todo esto viaja al
 * modelo, y un pegado accidental de media página web haría la consulta cara y
 * lenta sin agregar información útil.
 */
public record PeticionEvidencia(

        @Size(max = 20_000, message = "El texto de Instagram es demasiado largo.")
        String textoInstagram,

        @Size(max = 20_000, message = "El texto de la ficha de Google es demasiado largo.")
        String textoFichaGoogle,

        @Size(max = 5_000, message = "Las notas son demasiado largas.")
        String notas) {
}
