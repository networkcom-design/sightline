package com.networkcom.lupa.domain.auditoria;

import com.networkcom.lupa.domain.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Corregir los datos del comercio")
class CorregirDatosTest {

    private Auditoria auditoria() {
        Usuario usuario = Usuario.registrar("auditor@networkcom.com.ar", "hash", "Auditor");
        Auditoria auditoria = Auditoria.iniciar(usuario, "Barbería Don Ramón", "Barbería");
        auditoria.completarDatos("Resistencia", "3624 55-6677", "Av. Sarmiento 1240",
                "https://donramon.com.ar");
        return auditoria;
    }

    @Test
    @DisplayName("corregir un teléfono no dispara una remedición del sitio")
    void corregirElTelefonoNoTocaElSitio() {
        Auditoria auditoria = auditoria();

        boolean cambioElSitio = auditoria.corregirDatos(
                "Barbería Don Ramón", "Barbería", "Resistencia",
                "3624 11-2233", "Av. Sarmiento 1240", "https://donramon.com.ar");

        assertThat(cambioElSitio).isFalse();
        assertThat(auditoria.getTelefono()).isEqualTo("3624 11-2233");
    }

    @Test
    @DisplayName("cambiar el sitio avisa que hay que volver a medir")
    void cambiarElSitioPideRemedicion() {
        Auditoria auditoria = auditoria();

        boolean cambioElSitio = auditoria.corregirDatos(
                "Barbería Don Ramón", "Barbería", "Resistencia",
                "3624 55-6677", "Av. Sarmiento 1240", "https://otrositio.com.ar");

        assertThat(cambioElSitio).isTrue();
        assertThat(auditoria.getSitioWeb()).isEqualTo("https://otrositio.com.ar");
    }

    @Test
    @DisplayName("sacar el sitio también obliga a volver a medir")
    void quitarElSitioPideRemedicion() {
        Auditoria auditoria = auditoria();

        assertThat(auditoria.corregirDatos("Barbería Don Ramón", "Barbería", "Resistencia",
                null, null, "")).isTrue();
    }

    @Test
    @DisplayName("vacío y nulo son el mismo sitio: no hay cambio")
    void vacioYNuloSonLoMismo() {
        Usuario usuario = Usuario.registrar("a@b.com", "hash", "Auditor");
        Auditoria sinSitio = Auditoria.iniciar(usuario, "Comercio", "Rubro");
        sinSitio.completarDatos(null, null, null, null);

        // Un formulario manda cadena vacía donde el dominio tenía nulo. Si eso
        // contara como cambio, cada guardado dispararía una consulta de red
        // inútil contra un sitio que no existe.
        assertThat(sinSitio.corregirDatos("Comercio", "Rubro", null, null, null, "")).isFalse();
    }

    @Test
    @DisplayName("los espacios alrededor de la dirección no cuentan como cambio")
    void losEspaciosNoCuentan() {
        Auditoria auditoria = auditoria();

        assertThat(auditoria.corregirDatos("Barbería Don Ramón", "Barbería", "Resistencia",
                "3624 55-6677", "Av. Sarmiento 1240", "  https://donramon.com.ar  ")).isFalse();
    }

    @Test
    @DisplayName("el nombre y el rubro no se pueden vaciar")
    void nombreYRubroSiguenSiendoObligatorios() {
        Auditoria auditoria = auditoria();

        assertThatThrownBy(() -> auditoria.corregirDatos(
                "  ", "Barbería", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> auditoria.corregirDatos(
                "Barbería Don Ramón", "", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(auditoria.getNombre()).isEqualTo("Barbería Don Ramón");
        assertThat(auditoria.getRubro()).isEqualTo("Barbería");
    }

    @Test
    @DisplayName("corregir no borra las respuestas ya cargadas")
    void lasRespuestasSobreviven() {
        Auditoria auditoria = auditoria();
        auditoria.corregir(Senal.WA_BUSINESS, EstadoSenal.CUMPLE);

        auditoria.corregirDatos("Barbería Don Ramón S.R.L.", "Barbería", "Resistencia, Chaco",
                "3624 55-6677", "Av. Sarmiento 1240", "https://donramon.com.ar");

        assertThat(auditoria.respuestasParaElMotor())
                .containsEntry(Senal.WA_BUSINESS, EstadoSenal.CUMPLE);
        assertThat(auditoria.getNombre()).isEqualTo("Barbería Don Ramón S.R.L.");
    }
}
