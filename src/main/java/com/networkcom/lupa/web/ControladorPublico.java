package com.networkcom.lupa.web;

import com.networkcom.lupa.application.auditoria.ServicioAuditorias;
import com.networkcom.lupa.application.auditoria.dto.RespuestasAuditoria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El informe que ve el prospecto, sin cuenta y sin token de sesión.
 *
 * Devuelve un tipo propio y no el informe interno recortado: si fuera el mismo
 * objeto con campos ocultados, cualquier descuido futuro filtraría precios,
 * horas y márgenes al comercio que estás por cotizar.
 */
@RestController
@RequestMapping("/api/publico")
public class ControladorPublico {

    private final ServicioAuditorias servicio;

    public ControladorPublico(ServicioAuditorias servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/informes/{token}")
    public RespuestasAuditoria.VistaPublica informe(@PathVariable String token) {
        return servicio.vistaPublica(token);
    }
}
