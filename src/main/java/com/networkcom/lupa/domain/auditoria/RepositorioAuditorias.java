package com.networkcom.lupa.domain.auditoria;

import com.networkcom.lupa.domain.usuario.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de auditorías. Sin anotaciones: el dominio no sabe de JPA. */
public interface RepositorioAuditorias {

    Auditoria guardar(Auditoria auditoria);

    Optional<Auditoria> buscarPorId(UUID id);

    /** Busca por el token del enlace público. Es la puerta de la vista sin cuenta. */
    Optional<Auditoria> buscarPorToken(String token);

    List<Auditoria> deUsuario(Usuario usuario);

    void eliminar(Auditoria auditoria);
}
