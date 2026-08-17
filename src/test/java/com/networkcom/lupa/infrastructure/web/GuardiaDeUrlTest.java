package com.networkcom.lupa.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Guardia de URL")
class GuardiaDeUrlTest {

    @Test
    @DisplayName("al dominio pelado le pone https adelante")
    void completaElEsquema() {
        assertThat(GuardiaDeUrl.validar("networkcom.com.ar").toString())
                .isEqualTo("https://networkcom.com.ar");
    }

    @Test
    @DisplayName("respeta http cuando viene explícito")
    void respetaHttp() {
        assertThat(GuardiaDeUrl.validar("http://networkcom.com.ar").getScheme()).isEqualTo("http");
    }

    @ParameterizedTest
    @DisplayName("bloquea las direcciones que apuntan a la propia máquina o a la red interna")
    @ValueSource(strings = {
            "http://localhost:8080/actuator",
            "http://127.0.0.1/admin",
            "http://192.168.0.1",
            "http://10.0.0.5:5432",
            "http://169.254.169.254/latest/meta-data"
    })
    void bloqueaDireccionesInternas(String direccion) {
        assertThatThrownBy(() -> GuardiaDeUrl.validar(direccion))
                .isInstanceOf(GuardiaDeUrl.UrlNoPermitidaException.class)
                .hasMessageContaining("públicos");
    }

    @ParameterizedTest
    @DisplayName("rechaza esquemas que no son http ni https")
    @ValueSource(strings = {
            "file:///C:/Windows/System32/drivers/etc/hosts",
            "ftp://archivos.example.com",
            "gopher://example.com"
    })
    void rechazaEsquemasRaros(String direccion) {
        assertThatThrownBy(() -> GuardiaDeUrl.validar(direccion))
                .isInstanceOf(GuardiaDeUrl.UrlNoPermitidaException.class);
    }

    @Test
    @DisplayName("rechaza la dirección vacía")
    void rechazaVacio() {
        assertThatThrownBy(() -> GuardiaDeUrl.validar("   "))
                .isInstanceOf(GuardiaDeUrl.UrlNoPermitidaException.class)
                .hasMessageContaining("vacía");
    }

    @Test
    @DisplayName("rechaza dominios que no existen")
    void rechazaDominioInexistente() {
        assertThatThrownBy(() -> GuardiaDeUrl.validar("no-existe-este-dominio-lupa-12345.com.ar"))
                .isInstanceOf(GuardiaDeUrl.UrlNoPermitidaException.class)
                .hasMessageContaining("resolver");
    }
}
