package com.networkcom.lupa.domain.usuario;

/** Se intento registrar una cuenta con un email que ya existe. */
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String email) {
        super("Ya existe una cuenta registrada con el email " + email + ".");
    }
}
