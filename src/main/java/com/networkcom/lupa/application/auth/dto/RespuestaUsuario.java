package com.networkcom.lupa.application.auth.dto;

import com.networkcom.lupa.domain.usuario.Usuario;

import java.time.Instant;
import java.util.UUID;

/** Vista publica de un usuario. Nunca incluye el hash de la contrasena. */
public record RespuestaUsuario(UUID id, String nombre, String email, Instant creadoEn) {

    public static RespuestaUsuario desde(Usuario usuario) {
        return new RespuestaUsuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getCreadoEn());
    }
}
