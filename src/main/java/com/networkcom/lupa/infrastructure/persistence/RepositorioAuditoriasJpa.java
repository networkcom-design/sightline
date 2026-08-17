package com.networkcom.lupa.infrastructure.persistence;

import com.networkcom.lupa.domain.auditoria.Auditoria;
import com.networkcom.lupa.domain.auditoria.RepositorioAuditorias;
import com.networkcom.lupa.domain.usuario.Usuario;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioAuditoriasJpa implements RepositorioAuditorias {

    private final AuditoriaJpaRepository jpa;

    public RepositorioAuditoriasJpa(AuditoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Auditoria guardar(Auditoria auditoria) {
        return jpa.save(auditoria);
    }

    @Override
    public Optional<Auditoria> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Auditoria> buscarPorToken(String token) {
        return jpa.findByTokenPublico(token);
    }

    @Override
    public List<Auditoria> deUsuario(Usuario usuario) {
        return jpa.findByUsuarioIdOrderByCreadaEnDesc(usuario.getId());
    }

    @Override
    public void eliminar(Auditoria auditoria) {
        jpa.delete(auditoria);
    }
}
