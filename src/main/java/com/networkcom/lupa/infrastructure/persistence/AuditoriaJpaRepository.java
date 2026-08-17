package com.networkcom.lupa.infrastructure.persistence;

import com.networkcom.lupa.domain.auditoria.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditoriaJpaRepository extends JpaRepository<Auditoria, UUID> {

    Optional<Auditoria> findByTokenPublico(String tokenPublico);

    List<Auditoria> findByUsuarioIdOrderByCreadaEnDesc(UUID usuarioId);
}
