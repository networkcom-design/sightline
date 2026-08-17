package com.networkcom.lupa.web;

import com.networkcom.lupa.application.auth.ServicioAutenticacion;
import com.networkcom.lupa.application.auth.dto.PeticionLogin;
import com.networkcom.lupa.application.auth.dto.PeticionRegistro;
import com.networkcom.lupa.application.auth.dto.RespuestaAutenticacion;
import com.networkcom.lupa.application.auth.dto.RespuestaUsuario;
import com.networkcom.lupa.domain.usuario.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicio;

    public ControladorAutenticacion(ServicioAutenticacion servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<RespuestaAutenticacion> registro(@Valid @RequestBody PeticionRegistro peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(peticion));
    }

    @PostMapping("/login")
    public RespuestaAutenticacion login(@Valid @RequestBody PeticionLogin peticion) {
        return servicio.login(peticion);
    }

    /**
     * Devuelve el usuario del token. Sirve para que el frontend, al recargar la
     * pagina, confirme si el token guardado sigue siendo valido.
     */
    @GetMapping("/yo")
    public RespuestaUsuario yo(@AuthenticationPrincipal Usuario usuario) {
        return RespuestaUsuario.desde(usuario);
    }
}
