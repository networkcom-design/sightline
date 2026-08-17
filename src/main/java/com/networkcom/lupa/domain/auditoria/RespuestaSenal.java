package com.networkcom.lupa.domain.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/** La respuesta guardada de una señal dentro de una auditoría. */
@Entity
@Table(name = "respuestas_senal")
public class RespuestaSenal {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private Auditoria auditoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Senal senal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSenal estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenDeRespuesta origen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Dictamen.Confianza confianza;

    @Column(length = 300)
    private String fundamento;

    protected RespuestaSenal() {
    }

    RespuestaSenal(Auditoria auditoria, Dictamen dictamen) {
        this.id = UUID.randomUUID();
        this.auditoria = auditoria;
        this.senal = dictamen.senal();
        aplicar(dictamen);
    }

    /**
     * Sobrescribe la respuesta.
     *
     * Una corrección humana pisa cualquier dictamen anterior, pero un dictamen
     * de IA no pisa una respuesta humana: una vez que el auditor decidió, volver
     * a analizar no le puede cambiar la respuesta por debajo.
     */
    void aplicar(Dictamen dictamen) {
        if (this.origen == OrigenDeRespuesta.HUMANO && dictamen.origen() != OrigenDeRespuesta.HUMANO) {
            return;
        }
        this.estado = dictamen.estado();
        this.origen = dictamen.origen();
        this.confianza = dictamen.confianza();
        this.fundamento = dictamen.fundamento();
    }

    public Dictamen aDictamen() {
        return new Dictamen(senal, estado, origen, fundamento, confianza);
    }

    public UUID getId() {
        return id;
    }

    public Senal getSenal() {
        return senal;
    }

    public EstadoSenal getEstado() {
        return estado;
    }

    public OrigenDeRespuesta getOrigen() {
        return origen;
    }

    public Dictamen.Confianza getConfianza() {
        return confianza;
    }

    public String getFundamento() {
        return fundamento;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof RespuestaSenal respuesta)) {
            return false;
        }
        return Objects.equals(id, respuesta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
