package com.networkcom.lupa.domain.propuesta;

import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.Senal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Presupuesto editable")
class PresupuestoEditableTest {

    private Map<Senal, EstadoSenal> todoMal() {
        return Arrays.stream(Senal.values())
                .collect(Collectors.toMap(Function.identity(), s -> EstadoSenal.NO_CUMPLE, (a, b) -> a,
                        () -> new EnumMap<>(Senal.class)));
    }

    private GeneradorDePropuesta.Item item(GeneradorDePropuesta.Propuesta propuesta, String codigo) {
        return propuesta.items().stream()
                .filter(i -> i.servicio().codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("sin ajustes, todo entra al precio de lista")
    void porDefectoEntraTodo() {
        var propuesta = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS, Map.of());

        assertThat(propuesta.items()).allMatch(GeneradorDePropuesta.Item::incluido);
        assertThat(item(propuesta, "sitio-una-pagina").precio())
                .isEqualByComparingTo(CatalogoServicios.SITIO_UNA_PAGINA.precioReferenciaArs());
    }

    @Test
    @DisplayName("un servicio excluido sigue en la lista pero no se cobra ni se proyecta")
    void loExcluidoSeVePeroNoSuma() {
        var completa = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS, Map.of());

        var recortada = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS,
                Map.of("sitio-una-pagina", AjusteServicio.excluido()));

        // Sigue apareciendo: el auditor tiene que poder volver a sumarlo.
        assertThat(item(recortada, "sitio-una-pagina").incluido()).isFalse();
        assertThat(recortada.items()).hasSameSizeAs(completa.items());

        assertThat(recortada.totalUnico()).isLessThan(completa.totalUnico());
        assertThat(recortada.puntajeSostenido()).isLessThan(completa.puntajeSostenido());
        assertThat(recortada.hallazgosSinCubrir()).isNotEmpty();
    }

    @Test
    @DisplayName("cambiar el precio se refleja en el total y queda marcado como ajustado")
    void elPrecioEditadoManda() {
        var propuesta = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS,
                Map.of("whatsapp-business", new AjusteServicio(true, new BigDecimal("30000"))));

        var whatsapp = item(propuesta, "whatsapp-business");

        assertThat(whatsapp.precio()).isEqualByComparingTo("30000");
        assertThat(whatsapp.precio()).isNotEqualByComparingTo(
                CatalogoServicios.WHATSAPP_BUSINESS.precioReferenciaArs());
    }

    @Test
    @DisplayName("un precio ajustado no cambia el catálogo ni otras propuestas")
    void elAjusteEsDeEstaAuditoria() {
        GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS,
                Map.of("whatsapp-business", new AjusteServicio(true, new BigDecimal("1"))));

        var otra = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS, Map.of());

        assertThat(item(otra, "whatsapp-business").precio())
                .isEqualByComparingTo(CatalogoServicios.WHATSAPP_BUSINESS.precioReferenciaArs());
    }

    @Test
    @DisplayName("excluir un servicio único acorta el plazo de entrega")
    void sacarElSitioAcortaElPlazo() {
        var completa = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS, Map.of());
        var sinSitio = GeneradorDePropuesta.generar(todoMal(), CatalogoServicios.TODOS,
                Map.of("sitio-una-pagina", AjusteServicio.excluido()));

        assertThat(completa.plazoDiasEstimado())
                .isEqualTo(CatalogoServicios.SITIO_UNA_PAGINA.plazoDias());
        assertThat(sinSitio.plazoDiasEstimado()).isLessThan(completa.plazoDiasEstimado());
    }

    @Test
    @DisplayName("un ajuste de un servicio que no aplica a este comercio se ignora")
    void ajustarLoQueNoEstaNoRompe() {
        Map<Senal, EstadoSenal> casiTodoBien = Arrays.stream(Senal.values())
                .collect(Collectors.toMap(Function.identity(), s -> EstadoSenal.CUMPLE, (a, b) -> a,
                        () -> new EnumMap<>(Senal.class)));
        casiTodoBien.put(Senal.WA_CATALOGO, EstadoSenal.NO_CUMPLE);

        var propuesta = GeneradorDePropuesta.generar(casiTodoBien, CatalogoServicios.TODOS,
                Map.of("sitio-una-pagina", new AjusteServicio(true, new BigDecimal("999999"))));

        assertThat(propuesta.items()).hasSize(1);
        assertThat(propuesta.items().get(0).servicio().codigo()).isEqualTo("whatsapp-business");
        assertThat(propuesta.totalUnico())
                .isEqualByComparingTo(CatalogoServicios.WHATSAPP_BUSINESS.precioReferenciaArs());
    }
}
