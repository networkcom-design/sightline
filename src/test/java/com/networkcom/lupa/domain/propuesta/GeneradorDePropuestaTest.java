package com.networkcom.lupa.domain.propuesta;

import com.networkcom.lupa.domain.auditoria.Dimension;
import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.Senal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Generador de propuesta")
class GeneradorDePropuestaTest {

    private Map<Senal, EstadoSenal> todas(EstadoSenal estado) {
        return Arrays.stream(Senal.values())
                .collect(Collectors.toMap(Function.identity(), senal -> estado, (a, b) -> a,
                        () -> new EnumMap<>(Senal.class)));
    }

    @Test
    @DisplayName("el catálogo sabe cobrar todas las señales que la auditoría detecta")
    void elCatalogoCubreTodoElDiagnostico() {
        var cubiertas = CatalogoServicios.senalesCubiertas();

        assertThat(Arrays.asList(Senal.values()))
                .as("si una señal no la resuelve ningún servicio, se detecta un problema que no se sabe cotizar")
                .allMatch(cubiertas::contains);
    }

    @Test
    @DisplayName("un comercio que está impecable no genera propuesta")
    void sinHallazgosNoHayNadaQueVender() {
        var propuesta = GeneradorDePropuesta.generar(todas(EstadoSenal.CUMPLE), CatalogoServicios.TODOS);

        assertThat(propuesta.estaVacia()).isTrue();
        assertThat(propuesta.puntajeActual()).isEqualTo(100);
        assertThat(propuesta.mejoraAlEntregar()).isZero();
    }

    @Test
    @DisplayName("lo que se entrega puntúa menos que lo que se sostiene con el abono")
    void elAbonoAportaPuntajePropio() {
        var propuesta = GeneradorDePropuesta.generar(todas(EstadoSenal.NO_CUMPLE), CatalogoServicios.TODOS);

        assertThat(propuesta.items()).hasSize(CatalogoServicios.TODOS.size());
        assertThat(propuesta.puntajeActual()).isZero();

        // El numero creible para prometer es el de la entrega, no el techo.
        assertThat(propuesta.puntajeAlEntregar()).isLessThan(100);
        assertThat(propuesta.puntajeSostenido()).isEqualTo(100);
        assertThat(propuesta.loQueAportaElAbono()).isPositive();
        assertThat(propuesta.hallazgosSinCubrir()).isEmpty();
    }

    @Test
    @DisplayName("solo aparecen los servicios que resuelven algo")
    void noVendeLoQueNoHaceFalta() {
        Map<Senal, EstadoSenal> respuestas = todas(EstadoSenal.CUMPLE);
        Senal.de(Dimension.WHATSAPP).forEach(senal -> respuestas.put(senal, EstadoSenal.NO_CUMPLE));

        var propuesta = GeneradorDePropuesta.generar(respuestas, CatalogoServicios.TODOS);

        assertThat(propuesta.items()).hasSize(1);
        assertThat(propuesta.items().get(0).servicio().codigo()).isEqualTo("whatsapp-business");
        assertThat(propuesta.totalUnico()).isEqualByComparingTo(
                CatalogoServicios.WHATSAPP_BUSINESS.precioReferenciaArs());
        assertThat(propuesta.totalMensual()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("separa lo que se cobra una vez de lo que se cobra todos los meses")
    void separaUnicoDeMensual() {
        var propuesta = GeneradorDePropuesta.conServicios(
                todas(EstadoSenal.NO_CUMPLE),
                List.of(CatalogoServicios.WHATSAPP_BUSINESS, CatalogoServicios.CONTENIDO_MENSUAL));

        assertThat(propuesta.totalUnico())
                .isEqualByComparingTo(CatalogoServicios.WHATSAPP_BUSINESS.precioReferenciaArs());
        assertThat(propuesta.totalMensual())
                .isEqualByComparingTo(CatalogoServicios.CONTENIDO_MENSUAL.precioReferenciaArs());
    }

    @Test
    @DisplayName("sacar un servicio del presupuesto baja el puntaje sostenido")
    void resignarUnServicioSeVeEnElProyectado() {
        var respuestas = todas(EstadoSenal.NO_CUMPLE);

        var completa = GeneradorDePropuesta.generar(respuestas, CatalogoServicios.TODOS);
        var recortada = GeneradorDePropuesta.conServicios(respuestas,
                CatalogoServicios.TODOS.stream()
                        .filter(servicio -> !servicio.equals(CatalogoServicios.FICHA_GOOGLE))
                        .toList());

        assertThat(recortada.puntajeSostenido()).isLessThan(completa.puntajeSostenido());
        assertThat(recortada.hallazgosSinCubrir()).isNotEmpty();
    }

    @Test
    @DisplayName("el plazo es el del servicio único más largo, no la suma de todos")
    void losServiciosCorrenEnParalelo() {
        var propuesta = GeneradorDePropuesta.conServicios(
                todas(EstadoSenal.NO_CUMPLE),
                List.of(CatalogoServicios.WHATSAPP_BUSINESS, CatalogoServicios.SITIO_UNA_PAGINA));

        assertThat(propuesta.plazoDiasEstimado())
                .isEqualTo(CatalogoServicios.SITIO_UNA_PAGINA.plazoDias());
    }

    @Test
    @DisplayName("un abono mensual no estira el plazo de entrega")
    void elAbonoNoTienePlazoDeEntrega() {
        // Gestion de resenas es mensual y su ciclo de 30 dias no debe pisar al
        // del sitio, que se entrega en 21.
        var propuesta = GeneradorDePropuesta.conServicios(
                todas(EstadoSenal.NO_CUMPLE),
                List.of(CatalogoServicios.SITIO_UNA_PAGINA, CatalogoServicios.GESTION_RESENAS));

        assertThat(CatalogoServicios.GESTION_RESENAS.plazoDias())
                .isGreaterThan(CatalogoServicios.SITIO_UNA_PAGINA.plazoDias());
        assertThat(propuesta.plazoDiasEstimado())
                .isEqualTo(CatalogoServicios.SITIO_UNA_PAGINA.plazoDias());
    }

    @Test
    @DisplayName("una propuesta de puro abono no promete fecha de entrega")
    void soloAbonosNoTienenPlazo() {
        var propuesta = GeneradorDePropuesta.conServicios(
                todas(EstadoSenal.NO_CUMPLE),
                List.of(CatalogoServicios.CONTENIDO_MENSUAL, CatalogoServicios.GESTION_RESENAS));

        assertThat(propuesta.plazoDiasEstimado()).isZero();
        assertThat(propuesta.puntajeAlEntregar()).isEqualTo(propuesta.puntajeActual());
    }

    @Test
    @DisplayName("primero se presenta el servicio que resuelve más problemas")
    void ordenaPorImpactoResuelto() {
        var propuesta = GeneradorDePropuesta.generar(todas(EstadoSenal.NO_CUMPLE), CatalogoServicios.TODOS);

        var cantidades = propuesta.items().stream()
                .map(GeneradorDePropuesta.Item::hallazgosQueResuelve)
                .toList();

        assertThat(cantidades).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    @DisplayName("lo que ya está bien no se toca al proyectar")
    void laProyeccionNoRegalaPuntos() {
        Map<Senal, EstadoSenal> respuestas = todas(EstadoSenal.CUMPLE);
        respuestas.put(Senal.WA_CATALOGO, EstadoSenal.NO_CUMPLE);
        Senal.de(Dimension.CONTENIDO).forEach(senal -> respuestas.put(senal, EstadoSenal.NO_APLICA));

        var propuesta = GeneradorDePropuesta.generar(respuestas, CatalogoServicios.TODOS);

        // WhatsApp Business es de pago unico, asi que el arreglo entra en la
        // entrega y no depende de sostener ningun abono.
        assertThat(propuesta.puntajeAlEntregar()).isEqualTo(100);
        assertThat(propuesta.puntajeSostenido()).isEqualTo(100);
        assertThat(propuesta.items()).hasSize(1);
    }
}
