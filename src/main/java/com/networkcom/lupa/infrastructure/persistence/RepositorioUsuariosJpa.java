package com.networkcom.lupa.infrastructure.persistence;

import com.networkcom.lupa.domain.usuario.RepositorioUsuarios;
import com.networkcom.lupa.domain.usuario.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador que conecta el puerto del dominio con Spring Data.
 *
 * Ademas centraliza la normalizacion del email: todas las busquedas pasan por
 * minusculas, igual que el indice unico de la migracion.
 */
@Repository
public class RepositorioUsuariosJpa implements RepositorioUsuarios {

    private final UsuarioJpaRepository jpa;

    public RepositorioUsuariosJpa(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return jpa.save(usuario);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(Usuario.normalizarEmail(email));
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existeConEmail(String email) {
        return jpa.existsByEmail(Usuario.normalizarEmail(email));
    }
}
