package com.networkcom.lupa.domain.contacto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Teléfonos argentinos para WhatsApp")
class TelefonoArgentinoTest {

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("normaliza las formas en que la gente escribe un número")
    @CsvSource({
            // Resistencia, area 3624, como lo anota cualquiera.
            "'3624-556677',        5493624556677",
            "'3624 556677',        5493624556677",
            "'3624556677',         5493624556677",
            // Con el 0 de larga distancia y el 15 de celular.
            "'03624 15-556677',    5493624556677",
            "'0362415556677',      5493624556677",
            // Ya internacional, con y sin el 9 de movil.
            "'+54 9 3624 556677',  5493624556677",
            "'5493624556677',      5493624556677",
            "'543624556677',       5493624556677",
            // Buenos Aires, area de 2 digitos.
            "'011 4555-6677',      5491145556677",
            "'11 4555 6677',       5491145556677",
            "'011 15 4555-6677',   5491145556677",
            // Area de 4 digitos con separadores raros.
            "'(0388) 154-556677',  5493884556677"
    })
    void normalizaLoQueSea(String crudo, String esperado) {
        assertThat(TelefonoArgentino.paraWhatsApp(crudo)).contains(esperado);
    }

    @ParameterizedTest
    @DisplayName("devuelve vacío cuando el número no se puede interpretar")
    @ValueSource(strings = {
            "",
            "   ",
            "1234",
            "no tiene telefono",
            "3624-55",
            "36245566771234567"
    })
    void anteLaDudaNoInventa(String crudo) {
        assertThat(TelefonoArgentino.paraWhatsApp(crudo)).isEmpty();
    }

    @Test
    @DisplayName("un número nulo no rompe")
    void toleraNulo() {
        assertThat(TelefonoArgentino.paraWhatsApp(null)).isEmpty();
    }

    @Test
    @DisplayName("lo muestra en un formato que el auditor puede verificar de un vistazo")
    void seMuestraLegible() {
        String normalizado = TelefonoArgentino.paraWhatsApp("3624-556677").orElseThrow();

        assertThat(TelefonoArgentino.paraMostrar(normalizado)).isEqualTo("+54 9 3624 556677");
    }
}
