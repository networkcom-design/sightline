package com.networkcom.lupa.application.auth.dto;

/** Lo que recibe el frontend al registrarse o iniciar sesion. */
public record RespuestaAutenticacion(
        String token,
        String tipo,
        long expiraEnSegundos,
        RespuestaUsuario usuario) {

    public static RespuestaAutenticacion de(String token, long expiraEnSegundos, RespuestaUsuario usuario) {
        return new RespuestaAutenticacion(token, "Bearer", expiraEnSegundos, usuario);
    }
}
