package com.networkcom.lupa.infrastructure.persistence;

import com.networkcom.lupa.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repositorio de Spring Data. Lo usa el adaptador, no el resto de la aplicacion. */
public interface UsuarioJpaRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
