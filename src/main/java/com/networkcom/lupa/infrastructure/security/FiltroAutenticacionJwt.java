package com.networkcom.lupa.infrastructure.security;

import com.networkcom.lupa.domain.usuario.RepositorioUsuarios;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el token del encabezado Authorization y, si es valido, deja al usuario
 * autenticado para el resto de la peticion.
 *
 * Si no hay token o esta vencido, el filtro no corta la cadena ni devuelve un
 * error: simplemente no autentica, y despues Spring Security decide si el
 * endpoint pedido necesitaba autenticacion o era publico.
 */
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String ENCABEZADO = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt servicioJwt;
    private final RepositorioUsuarios repositorioUsuarios;

    public FiltroAutenticacionJwt(ServicioJwt servicioJwt, RepositorioUsuarios repositorioUsuarios) {
        this.servicioJwt = servicioJwt;
        this.repositorioUsuarios = repositorioUsuarios;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest peticion,
            @NonNull HttpServletResponse respuesta,
            @NonNull FilterChain cadena) throws ServletException, IOException {

        extraerToken(peticion)
                .flatMap(servicioJwt::validarYObtenerUsuarioId)
                .flatMap(repositorioUsuarios::buscarPorId)
                .ifPresent(usuario -> {
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            usuario, null, List.of());
                    autenticacion.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(peticion));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                });

        cadena.doFilter(peticion, respuesta);
    }

    private java.util.Optional<String> extraerToken(HttpServletRequest peticion) {
        String encabezado = peticion.getHeader(ENCABEZADO);

        if (encabezado == null || !encabezado.startsWith(PREFIJO)) {
            return java.util.Optional.empty();
        }

        String token = encabezado.substring(PREFIJO.length()).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }
}
