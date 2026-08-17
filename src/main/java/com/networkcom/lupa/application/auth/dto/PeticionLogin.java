package com.networkcom.lupa.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de acceso.
 *
 * A diferencia del registro, aca no se valida formato ni largo: si los datos
 * estan mal, la respuesta tiene que ser la misma "email o contrasena
 * incorrectos" de siempre. Un mensaje de validacion distinto le confirmaria a
 * un atacante que ese email existe.
 */
public record PeticionLogin(

        @NotBlank(message = "El email no puede estar vacio.")
        String email,

        @NotBlank(message = "La contrasena no puede estar vacia.")
        String contrasena) {
}
