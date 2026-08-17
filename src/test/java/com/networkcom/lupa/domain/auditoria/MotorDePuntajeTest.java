package com.networkcom.lupa.domain.auditoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Motor de puntaje")
class MotorDePuntajeTest {

    private Map<Senal, EstadoSenal> todas(EstadoSenal estado) {
        return Arrays.stream(Senal.values())
                .collect(Collectors.toMap(Function.identity(), senal -> estado, (a, b) -> a,
                        () -> new EnumMap<>(Senal.class)));
    }

    @Test
    @DisplayName("los pesos de las dimensiones suman 100")
    void pesosBalanceados() {
        assertThatCode(Dimension::validarPesos).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("un comercio que cumple todo puntúa 100 y queda como referente")
    void todoCumple() {
        var resultado = MotorDePuntaje.calcular(todas(EstadoSenal.CUMPLE));

        assertThat(resultado.puntajeGlobal()).isEqualTo(100);
        assertThat(resultado.nivel()).isEqualTo(NivelPresencia.REFERENTE);
        assertThat(resultado.planDeAccion()).isEmpty();
        assertThat(resultado.minutosTotalesEstimados()).isZero();
    }

    @Test
    @DisplayName("un comercio que no cumple nada puntúa 0 y queda en crítico")
    void nadaCumple() {
        var resultado = MotorDePuntaje.calcular(todas(EstadoSenal.NO_CUMPLE));

        assertThat(resultado.puntajeGlobal()).isZero();
        assertThat(resultado.nivel()).isEqualTo(NivelPresencia.CRITICO);
        assertThat(resultado.planDeAccion()).hasSize(Senal.values().length);
    }

    @Test
    @DisplayName("lo que no aplica no resta: sale del cálculo en vez de contar como incumplido")
    void loQueNoAplicaNoCastiga() {
        Map<Senal, EstadoSenal> respuestas = todas(EstadoSenal.CUMPLE);
        respuestas.put(Senal.NAP_DIRECCION, EstadoSenal.NO_APLICA);
        respuestas.put(Senal.G_FOTOS, EstadoSenal.NO_APLICA);

        var resultado = MotorDePuntaje.calcular(respuestas);

        assertThat(resultado.puntajeGlobal()).isEqualTo(100);
        assertThat(resultado.senalesEvaluadas()).isEqualTo(Senal.values().length - 2);
    }

    @Test
    @DisplayName("el plan pone primero lo de mucho impacto y poco esfuerzo")
    void elPlanPriorizaLosArreglosRapidos() {
        var resultado = MotorDePuntaje.calcular(todas(EstadoSenal.NO_CUMPLE));

        var primero = resultado.planDeAccion().get(0);
        var ultimo = resultado.planDeAccion().get(resultado.planDeAccion().size() - 1);

        double prioridadPrimero = (double) primero.impacto().getValor() / primero.esfuerzo().getValor();
        double prioridadUltimo = (double) ultimo.impacto().getValor() / ultimo.esfuerzo().getValor();

        assertThat(prioridadPrimero).isGreaterThan(prioridadUltimo);
        assertThat(primero.impacto()).isEqualTo(Impacto.ALTO);
        assertThat(primero.esfuerzo()).isEqualTo(Esfuerzo.BAJO);
    }

    @Test
    @DisplayName("fallar solo la ficha de Google pesa más que fallar solo el contenido")
    void lasDimensionesNoPesanIgual() {
        Map<Senal, EstadoSenal> sinGoogle = todas(EstadoSenal.CUMPLE);
        Senal.de(Dimension.GOOGLE).forEach(senal -> sinGoogle.put(senal, EstadoSenal.NO_CUMPLE));

        Map<Senal, EstadoSenal> sinContenido = todas(EstadoSenal.CUMPLE);
        Senal.de(Dimension.CONTENIDO).forEach(senal -> sinContenido.put(senal, EstadoSenal.NO_CUMPLE));

        int conGoogleRoto = MotorDePuntaje.calcular(sinGoogle).puntajeGlobal();
        int conContenidoRoto = MotorDePuntaje.calcular(sinContenido).puntajeGlobal();

        assertThat(conGoogleRoto).isEqualTo(75);
        assertThat(conContenidoRoto).isEqualTo(93);
        assertThat(conGoogleRoto).isLessThan(conContenidoRoto);
    }

    @Test
    @DisplayName("el informe estima cuánto trabajo hay por delante")
    void estimaElTrabajoPendiente() {
        Map<Senal, EstadoSenal> respuestas = todas(EstadoSenal.CUMPLE);
        respuestas.put(Senal.G_TELEFONO, EstadoSenal.NO_CUMPLE);
        respuestas.put(Senal.IG_ENLACE, EstadoSenal.NO_CUMPLE);

        var resultado = MotorDePuntaje.calcular(respuestas);

        assertThat(resultado.planDeAccion()).hasSize(2);
        assertThat(resultado.minutosTotalesEstimados())
                .isEqualTo(Senal.G_TELEFONO.getMinutosEstimados() + Senal.IG_ENLACE.getMinutosEstimados());
    }

    @Test
    @DisplayName("cada dimensión reporta cuántas señales cumplió")
    void detallePorDimension() {
        var resultado = MotorDePuntaje.calcular(todas(EstadoSenal.CUMPLE));

        assertThat(resultado.dimensiones()).hasSize(Dimension.values().length);
        assertThat(resultado.dimensiones()).allSatisfy(dimension -> {
            assertThat(dimension.puntaje()).isEqualTo(100);
            assertThat(dimension.senalesCumplidas()).isEqualTo(dimension.senalesEvaluadas());
        });
    }
}
