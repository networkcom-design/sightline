package com.networkcom.lupa.domain.usuario;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de usuarios.
 *
 * Es una interfaz sin anotaciones de Spring ni de JPA a proposito: el dominio
 * declara que necesita guardar y buscar usuarios, y la infraestructura decide
 * como. Asi el dia que cambie la base de datos, el dominio no se entera.
 */
public interface RepositorioUsuarios {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(UUID id);

    boolean existeConEmail(String email);
}
