package com.networkcom.lupa.domain.usuario;

/**
 * El email no existe o la contrasena no coincide.
 *
 * Es una sola excepcion para los dos casos a proposito: si el mensaje
 * distinguiera "ese email no existe" de "la contrasena esta mal", cualquiera
 * podria averiguar que cuentas estan registradas probando emails.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("El email o la contrasena no son correctos.");
    }
}
