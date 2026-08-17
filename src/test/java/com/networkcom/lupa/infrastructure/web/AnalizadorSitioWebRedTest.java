package com.networkcom.lupa.infrastructure.web;

import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.Senal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas que salen a internet de verdad.
 *
 * Están fuera del build normal porque dependen de sitios ajenos: si mañana uno
 * cambia el título, la compilación no tiene por qué romperse. Se corren a mano
 * para verificar que el analizador mide de verdad y no solo en teoría.
 *
 * mvn test -Dgroups=red -DexcludedGroups=
 */
@Tag("red")
@DisplayName("Analizador contra sitios reales")
class AnalizadorSitioWebRedTest {

    private final AnalizadorSitioWeb analizador = new AnalizadorSitioWeb();

    @Test
    @DisplayName("mide un sitio real y reporta lo que encontró")
    void mideUnSitioReal() {
        var resultado = analizador.analizar("example.com");

        System.out.println("Medición: " + resultado.medicion());
        resultado.senales().forEach((senal, estado) ->
                System.out.println("  " + senal + " -> " + estado));

        assertThat(resultado.medicion().alcanzable()).isTrue();
        assertThat(resultado.medicion().codigoHttp()).isEqualTo(200);
        assertThat(resultado.medicion().milisegundos()).isPositive();
        assertThat(resultado.medicion().titulo()).isNotBlank();
        assertThat(resultado.senales()).containsKeys(Senal.automaticas().toArray(new Senal[0]));
    }

    @Test
    @DisplayName("un sitio que no existe no rompe: queda como no alcanzable")
    void sitioInexistente() {
        var resultado = analizador.analizar("no-existe-este-dominio-lupa-98765.com.ar");

        assertThat(resultado.medicion().alcanzable()).isFalse();
        assertThat(resultado.medicion().error()).isNotBlank();
        assertThat(resultado.senales().get(Senal.WEB_RESPONDE)).isEqualTo(EstadoSenal.NO_CUMPLE);
    }

    @Test
    @DisplayName("sin sitio web, todas las señales del sitio quedan incumplidas")
    void sinSitio() {
        var resultado = analizador.analizar("");

        assertThat(resultado.medicion().alcanzable()).isFalse();
        assertThat(resultado.senales().values()).allMatch(estado -> estado == EstadoSenal.NO_CUMPLE);
    }
}
