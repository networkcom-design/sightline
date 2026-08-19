package com.networkcom.lupa.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un usuario de Sightline, que en la practica es quien audita: alguien de la
 * agencia, no el comercio auditado.
 *
 * La entidad nunca guarda la contrasena en claro: recibe y expone unicamente el
 * hash. Quien la calcula es la capa de aplicacion, porque el algoritmo de
 * hashing es una decision de infraestructura y no del dominio.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "contrasena_hash", nullable = false, length = 72)
    private String contrasenaHash;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    /** Constructor que exige JPA. No usarlo desde el codigo de la aplicacion. */
    protected Usuario() {
    }

    private Usuario(UUID id, String email, String contrasenaHash, String nombre, Instant creadoEn) {
        this.id = id;
        this.email = email;
        this.contrasenaHash = contrasenaHash;
        this.nombre = nombre;
        this.creadoEn = creadoEn;
    }

    /**
     * Crea un usuario nuevo. El email se guarda normalizado en minusculas para
     * que el indice unico de la base y las busquedas coincidan siempre.
     */
    public static Usuario registrar(String email, String contrasenaHash, String nombre) {
        return new Usuario(
                UUID.randomUUID(),
                normalizarEmail(email),
                contrasenaHash,
                nombre.trim(),
                Instant.now());
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public String getNombre() {
        return nombre;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Usuario usuario)) {
            return false;
        }
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
