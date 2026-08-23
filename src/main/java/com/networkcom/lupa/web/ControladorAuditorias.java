package com.networkcom.lupa.web;

import com.networkcom.lupa.application.auditoria.ServicioAuditorias;
import com.networkcom.lupa.application.auditoria.dto.PeticionEvidencia;
import com.networkcom.lupa.application.auditoria.dto.PeticionNuevaAuditoria;
import com.networkcom.lupa.application.auditoria.dto.RespuestasAuditoria;
import com.networkcom.lupa.application.mensaje.CanalDeEnvio;
import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.propuesta.EstadoTarea;
import com.networkcom.lupa.domain.usuario.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias")
public class ControladorAuditorias {

    private final ServicioAuditorias servicio;

    public ControladorAuditorias(ServicioAuditorias servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestasAuditoria.Resumen> listar(@AuthenticationPrincipal Usuario usuario) {
        return servicio.listar(usuario);
    }

    @PostMapping
    public ResponseEntity<RespuestasAuditoria.Informe> crear(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody PeticionNuevaAuditoria peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(usuario, peticion));
    }

    @GetMapping("/{id}")
    public RespuestasAuditoria.Informe informe(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id) {
        return servicio.informe(usuario, id);
    }

    /** Corrige los datos del comercio. Si cambia el sitio, se vuelve a medir. */
    @PutMapping("/{id}/datos")
    public RespuestasAuditoria.Informe corregirDatos(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @Valid @RequestBody PeticionNuevaAuditoria peticion) {
        return servicio.corregirDatos(usuario, id, peticion);
    }

    @PostMapping("/{id}/evidencia")
    public RespuestasAuditoria.Informe analizarEvidencia(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @Valid @RequestBody PeticionEvidencia peticion) {
        return servicio.analizarEvidencia(usuario, id, peticion);
    }

    /** Corrección del auditor sobre una señal. Es la última palabra. */
    @PutMapping("/{id}/senales/{codigo}")
    public RespuestasAuditoria.Informe corregir(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @PathVariable String codigo,
            @RequestBody CambioDeSenal cambio) {
        return servicio.corregir(usuario, id, codigo, cambio.estado());
    }

    /** Incluye o saca un servicio del presupuesto, y opcionalmente le cambia el precio. */
    @PutMapping("/{id}/propuesta/servicios/{codigo}")
    public RespuestasAuditoria.Informe ajustarServicio(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @PathVariable String codigo,
            @RequestBody AjusteDeServicio ajuste) {
        return servicio.ajustarServicio(usuario, id, codigo, ajuste.incluido(), ajuste.precio());
    }

    /**
     * Devuelve el mensaje listo para mandar. No manda nada.
     *
     * `origen` es la dirección desde la que se sirve el frontend, y se usa para
     * armar el enlace público. Viaja como parámetro porque el backend no sabe
     * bajo qué dominio está publicada la aplicación.
     */
    @GetMapping("/{id}/mensaje")
    public RespuestasAuditoria.MensajeParaEnviar mensaje(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @RequestParam CanalDeEnvio canal,
            @RequestParam String origen) {
        return servicio.mensaje(usuario, id, canal, origen);
    }

    @PostMapping("/{id}/enviada")
    public RespuestasAuditoria.Informe marcarEnviada(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id) {
        return servicio.marcarEnviada(usuario, id);
    }

    /** El cliente aceptó: congela el presupuesto y crea las tareas a seguir. */
    @PostMapping("/{id}/propuesta/aceptar")
    public RespuestasAuditoria.Informe aceptarPropuesta(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id) {
        return servicio.aceptarPropuesta(usuario, id);
    }

    @PostMapping("/{id}/propuesta/rechazar")
    public RespuestasAuditoria.Informe rechazarPropuesta(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @Valid @RequestBody MotivoDeRechazo motivo) {
        return servicio.rechazarPropuesta(usuario, id, motivo.motivo());
    }

    /** Mueve una tarea contratada. El estado del expediente se recalcula solo. */
    @PutMapping("/{id}/tareas/{codigo}")
    public RespuestasAuditoria.Informe cambiarEstadoDeTarea(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id,
            @PathVariable String codigo,
            @RequestBody CambioDeTarea cambio) {
        return servicio.cambiarEstadoDeTarea(usuario, id, codigo, cambio.estado(), cambio.nota());
    }

    @PostMapping("/{id}/revisada")
    public RespuestasAuditoria.Informe marcarRevisada(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id) {
        return servicio.marcarRevisada(usuario, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable UUID id) {
        servicio.eliminar(usuario, id);
        return ResponseEntity.noContent().build();
    }

    public record CambioDeSenal(EstadoSenal estado) {
    }

    /** `precio` en nulo vuelve el servicio al valor de lista. */
    public record AjusteDeServicio(boolean incluido, BigDecimal precio) {
    }

    public record CambioDeTarea(EstadoTarea estado, String nota) {
    }

    /**
     * El motivo es obligatorio a propósito: una propuesta perdida sin razón
     * anotada no le sirve a nadie dentro de tres meses.
     */
    public record MotivoDeRechazo(
            @NotBlank(message = "Anotá por qué no avanzó: es lo que sirve para la próxima.")
            @Size(max = 400, message = "El motivo no puede pasar de 400 caracteres.")
            String motivo) {
    }
}
