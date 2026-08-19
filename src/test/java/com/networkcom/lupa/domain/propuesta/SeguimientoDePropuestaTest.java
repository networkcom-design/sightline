package com.networkcom.lupa.domain.propuesta;

import com.networkcom.lupa.domain.auditoria.Auditoria;
import com.networkcom.lupa.domain.auditoria.EstadoAuditoria;
import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.Senal;
import com.networkcom.lupa.domain.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Seguimiento de una propuesta aceptada")
class SeguimientoDePropuestaTest {

    private Auditoria auditoriaConTodoMal() {
        Usuario usuario = Usuario.registrar("auditor@networkcom.com.ar", "hash", "Auditor");
        return Auditoria.iniciar(usuario, "Barbería Don Ramón", "Barbería");
    }

    private Map<Senal, EstadoSenal> todoMal() {
        return Arrays.stream(Senal.values())
                .collect(Collectors.toMap(Function.identity(), s -> EstadoSenal.NO_CUMPLE, (a, b) -> a,
                        () -> new EnumMap<>(Senal.class)));
    }

    private List<GeneradorDePropuesta.Item> propuestaCompleta() {
        return GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS, Map.of()).items();
    }

    private TareaContratada tarea(Auditoria auditoria, String codigo) {
        return auditoria.getTareas().stream()
                .filter(tarea -> tarea.getCodigoServicio().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    private String algunServicioUnico(Auditoria auditoria) {
        return auditoria.getTareas().stream()
                .filter(tarea -> !tarea.esRecurrente())
                .findFirst()
                .orElseThrow()
                .getCodigoServicio();
    }

    @Test
    @DisplayName("aceptar crea una tarea por cada servicio incluido")
    void aceptarCreaLasTareas() {
        Auditoria auditoria = auditoriaConTodoMal();
        List<GeneradorDePropuesta.Item> items = propuestaCompleta();

        auditoria.aceptarPropuesta(items);

        assertThat(auditoria.getEstado()).isEqualTo(EstadoAuditoria.ACEPTADA);
        assertThat(auditoria.getTareas()).hasSameSizeAs(items);
        assertThat(auditoria.getTareas()).allMatch(tarea -> tarea.getEstado() == EstadoTarea.PENDIENTE);
        assertThat(auditoria.getAceptadaEn()).isNotNull();
    }

    @Test
    @DisplayName("los servicios excluidos no se contratan")
    void loExcluidoNoEntra() {
        Auditoria auditoria = auditoriaConTodoMal();

        var items = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS,
                Map.of("sitio-una-pagina", AjusteServicio.excluido())).items();

        auditoria.aceptarPropuesta(items);

        assertThat(auditoria.getTareas())
                .extracting(TareaContratada::getCodigoServicio)
                .doesNotContain("sitio-una-pagina");
    }

    @Test
    @DisplayName("el precio queda congelado: cambiar el ajuste después no reescribe lo pactado")
    void elPrecioPactadoNoSeMueve() {
        Auditoria auditoria = auditoriaConTodoMal();

        var items = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS,
                Map.of("whatsapp-business", new AjusteServicio(true, new BigDecimal("30000")))).items();

        auditoria.aceptarPropuesta(items);

        // El presupuesto está cerrado: ni siquiera se deja intentar el cambio.
        assertThatThrownBy(() -> auditoria.ajustarServicio("whatsapp-business", true, new BigDecimal("99999")))
                .isInstanceOf(Auditoria.PropuestaCerradaException.class);

        assertThat(tarea(auditoria, "whatsapp-business").getPrecio()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("aceptar dos veces se rechaza para no borrar el avance")
    void noSeAceptaDosVeces() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        String codigo = algunServicioUnico(auditoria);
        auditoria.cambiarEstadoDeTarea(codigo, EstadoTarea.COMPLETADA, null);

        assertThatThrownBy(() -> auditoria.aceptarPropuesta(propuestaCompleta()))
                .isInstanceOf(Auditoria.PropuestaCerradaException.class);

        assertThat(tarea(auditoria, codigo).getEstado()).isEqualTo(EstadoTarea.COMPLETADA);
    }

    @Test
    @DisplayName("mover una tarea pasa el expediente a en ejecución")
    void arrancarUnaTareaMueveElExpediente() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        auditoria.cambiarEstadoDeTarea(algunServicioUnico(auditoria), EstadoTarea.EN_CURSO, "Arrancamos por acá");

        assertThat(auditoria.getEstado()).isEqualTo(EstadoAuditoria.EN_EJECUCION);
        assertThat(auditoria.avanceDelTrabajo()).isLessThan(100);
    }

    @Test
    @DisplayName("con todo cumplido el expediente se cierra solo")
    void terminarTodoCierraElExpediente() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        auditoria.getTareas().forEach(tarea ->
                auditoria.cambiarEstadoDeTarea(tarea.getCodigoServicio(), EstadoTarea.COMPLETADA, null));

        assertThat(auditoria.getEstado()).isEqualTo(EstadoAuditoria.ENTREGADA);
        assertThat(auditoria.avanceDelTrabajo()).isEqualTo(100);
        assertThat(auditoria.getEntregadaEn()).isNotNull();
    }

    @Test
    @DisplayName("un abono en curso ya cuenta como cumplido; uno único no")
    void elAbonoEnCursoYaEstaEntregando() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        TareaContratada recurrente = auditoria.getTareas().stream()
                .filter(TareaContratada::esRecurrente)
                .findFirst()
                .orElseThrow();

        auditoria.cambiarEstadoDeTarea(recurrente.getCodigoServicio(), EstadoTarea.EN_CURSO, null);
        assertThat(tarea(auditoria, recurrente.getCodigoServicio()).estaCumplida()).isTrue();

        String unico = algunServicioUnico(auditoria);
        auditoria.cambiarEstadoDeTarea(unico, EstadoTarea.EN_CURSO, null);
        assertThat(tarea(auditoria, unico).estaCumplida()).isFalse();
    }

    @Test
    @DisplayName("volver una tarea atrás reabre el expediente sin perder la fecha de inicio")
    void volverAtrasReabreElExpediente() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        auditoria.getTareas().forEach(tarea ->
                auditoria.cambiarEstadoDeTarea(tarea.getCodigoServicio(), EstadoTarea.COMPLETADA, null));

        String codigo = algunServicioUnico(auditoria);
        var inicioOriginal = tarea(auditoria, codigo).getIniciadaEn();

        auditoria.cambiarEstadoDeTarea(codigo, EstadoTarea.EN_CURSO, "El cliente pidió un cambio");

        assertThat(auditoria.getEstado()).isEqualTo(EstadoAuditoria.EN_EJECUCION);
        assertThat(auditoria.getEntregadaEn()).isNull();
        assertThat(tarea(auditoria, codigo).getIniciadaEn()).isEqualTo(inicioOriginal);
        assertThat(tarea(auditoria, codigo).getNota()).isEqualTo("El cliente pidió un cambio");
    }

    @Test
    @DisplayName("completar tareas no toca las señales de la auditoría")
    void elTrabajoHechoNoInventaPuntaje() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        // La auditoría arranca sin ninguna respuesta cargada: si ejecutar el
        // trabajo marcara señales solo, acá aparecerían respuestas de la nada.
        int respuestasAntes = auditoria.getRespuestas().size();

        auditoria.getTareas().forEach(tarea ->
                auditoria.cambiarEstadoDeTarea(tarea.getCodigoServicio(), EstadoTarea.COMPLETADA, null));

        assertThat(auditoria.getRespuestas()).hasSize(respuestasAntes);
    }

    @Test
    @DisplayName("el total contratado sale de los precios congelados")
    void elTotalSaleDeLoPactado() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        BigDecimal esperadoUnico = auditoria.getTareas().stream()
                .filter(tarea -> !tarea.esRecurrente())
                .map(TareaContratada::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(auditoria.totalContratado(false)).isEqualByComparingTo(esperadoUnico);
        assertThat(auditoria.totalContratado(true)).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("rechazar exige un motivo")
    void rechazarSinMotivoNoVa() {
        Auditoria auditoria = auditoriaConTodoMal();

        assertThatThrownBy(() -> auditoria.rechazarPropuesta("  "))
                .isInstanceOf(IllegalArgumentException.class);

        auditoria.rechazarPropuesta("Le pareció caro el sitio");

        assertThat(auditoria.getEstado()).isEqualTo(EstadoAuditoria.RECHAZADA);
        assertThat(auditoria.getMotivoRechazo()).isEqualTo("Le pareció caro el sitio");
        assertThat(auditoria.getRechazadaEn()).isNotNull();
    }

    @Test
    @DisplayName("un rechazo se puede revertir si el cliente vuelve")
    void elClienteQueVuelve() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.rechazarPropuesta("Lo va a pensar");

        auditoria.aceptarPropuesta(propuestaCompleta());

        assertThat(auditoria.getEstado()).isEqualTo(EstadoAuditoria.ACEPTADA);
        assertThat(auditoria.getMotivoRechazo()).isNull();
        assertThat(auditoria.getRechazadaEn()).isNull();
    }

    @Test
    @DisplayName("no se puede aceptar una propuesta sin ningún servicio")
    void aceptarNadaNoTieneSentido() {
        Auditoria auditoria = auditoriaConTodoMal();

        assertThatThrownBy(() -> auditoria.aceptarPropuesta(List.of()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(auditoria.getEstado()).isEqualTo(EstadoAuditoria.BORRADOR);
    }

    @Test
    @DisplayName("mover una tarea que el comercio no contrató falla")
    void tareaInexistente() {
        Auditoria auditoria = auditoriaConTodoMal();
        auditoria.aceptarPropuesta(propuestaCompleta());

        assertThatThrownBy(() ->
                auditoria.cambiarEstadoDeTarea("servicio-que-no-existe", EstadoTarea.EN_CURSO, null))
                .hasMessageContaining("servicio-que-no-existe");
    }
}
