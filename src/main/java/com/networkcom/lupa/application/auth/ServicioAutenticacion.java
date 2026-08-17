package com.networkcom.lupa.application.auth;

import com.networkcom.lupa.application.auth.dto.PeticionLogin;
import com.networkcom.lupa.application.auth.dto.PeticionRegistro;
import com.networkcom.lupa.application.auth.dto.RespuestaAutenticacion;
import com.networkcom.lupa.application.auth.dto.RespuestaUsuario;
import com.networkcom.lupa.domain.usuario.CredencialesInvalidasException;
import com.networkcom.lupa.domain.usuario.EmailYaRegistradoException;
import com.networkcom.lupa.domain.usuario.RepositorioUsuarios;
import com.networkcom.lupa.domain.usuario.Usuario;
import com.networkcom.lupa.infrastructure.security.ServicioJwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioAutenticacion {

    /**
     * Hash de descarte con el formato de BCrypt.
     *
     * Cuando el email no existe igual se ejecuta una comparacion contra este
     * valor. Sin eso, un login con email inexistente responderia mucho mas
     * rapido que uno con email valido, y esa diferencia de tiempo alcanza para
     * ir descubriendo que cuentas estan registradas.
     */
    private static final String HASH_SENUELO =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEe.Ie9tGLhVBiNBOVXpBqiVYYPTQ7Bnq2S";

    private final RepositorioUsuarios repositorioUsuarios;
    private final PasswordEncoder codificador;
    private final ServicioJwt servicioJwt;

    public ServicioAutenticacion(
            RepositorioUsuarios repositorioUsuarios,
            PasswordEncoder codificador,
            ServicioJwt servicioJwt) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.codificador = codificador;
        this.servicioJwt = servicioJwt;
    }

    @Transactional
    public RespuestaAutenticacion registrar(PeticionRegistro peticion) {
        String email = Usuario.normalizarEmail(peticion.email());

        if (repositorioUsuarios.existeConEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }

        Usuario usuario = repositorioUsuarios.guardar(
                Usuario.registrar(email, codificador.encode(peticion.contrasena()), peticion.nombre()));

        return emitirRespuesta(usuario);
    }

    @Transactional(readOnly = true)
    public RespuestaAutenticacion login(PeticionLogin peticion) {
        var encontrado = repositorioUsuarios.buscarPorEmail(peticion.email());

        String hashAComparar = encontrado
                .map(Usuario::getContrasenaHash)
                .orElse(HASH_SENUELO);

        boolean coincide = codificador.matches(peticion.contrasena(), hashAComparar);

        if (encontrado.isEmpty() || !coincide) {
            throw new CredencialesInvalidasException();
        }

        return emitirRespuesta(encontrado.get());
    }

    private RespuestaAutenticacion emitirRespuesta(Usuario usuario) {
        String token = servicioJwt.emitir(usuario.getId(), usuario.getEmail());
        return RespuestaAutenticacion.de(token, servicioJwt.segundosDeVida(), RespuestaUsuario.desde(usuario));
    }
}
