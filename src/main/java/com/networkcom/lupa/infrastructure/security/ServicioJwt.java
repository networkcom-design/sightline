package com.networkcom.lupa.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emision y verificacion de los tokens de acceso.
 *
 * Se firma con HS256, que necesita una clave de al menos 256 bits. Si el
 * secreto configurado es mas corto, la aplicacion no arranca: es preferible
 * fallar al inicio que emitir tokens debiles sin que nadie se entere.
 */
@Service
public class ServicioJwt {

    private static final int LARGO_MINIMO_SECRETO = 32;

    private final SecretKey clave;
    private final Duration duracion;

    public ServicioJwt(PropiedadesLupa propiedades) {
        byte[] bytes = propiedades.jwt().secreto().getBytes(StandardCharsets.UTF_8);

        if (bytes.length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException(
                    "El secreto de firma JWT necesita al menos " + LARGO_MINIMO_SECRETO
                            + " caracteres. Configura lupa_JWT_SECRET con un valor mas largo.");
        }

        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofHours(propiedades.jwt().duracionHoras());
    }

    public String emitir(UUID usuarioId, String email) {
        Instant ahora = Instant.now();

        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("email", email)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    /**
     * Devuelve el id del usuario si el token es valido, o vacio si esta vencido,
     * mal firmado o directamente no es un JWT.
     */
    public Optional<UUID> validarYObtenerUsuarioId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long segundosDeVida() {
        return duracion.toSeconds();
    }
}
