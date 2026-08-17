package com.networkcom.lupa.application.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cuenta.
 *
 * El maximo de 72 caracteres en la contrasena no es arbitrario: BCrypt ignora
 * todo lo que pase de 72 bytes. Sin este limite, alguien podria elegir una
 * contrasena larguisima creyendo que es mas segura mientras el algoritmo
 * descarta el final en silencio.
 */
public record PeticionRegistro(

        @NotBlank(message = "El nombre no puede estar vacio.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String nombre,

        @NotBlank(message = "El email no puede estar vacio.")
        @Email(message = "El email no tiene un formato valido.")
        @Size(max = 180, message = "El email no puede superar los 180 caracteres.")
        String email,

        @NotBlank(message = "La contrasena no puede estar vacia.")
        @Size(min = 8, max = 72, message = "La contrasena tiene que tener entre 8 y 72 caracteres.")
        String contrasena) {
}
