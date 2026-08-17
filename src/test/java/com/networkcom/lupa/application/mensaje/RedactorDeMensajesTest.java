package com.networkcom.lupa.application.mensaje;

import com.networkcom.lupa.application.ia.ProveedorIA;
import com.networkcom.lupa.domain.auditoria.Auditoria;
import com.networkcom.lupa.domain.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Redactor de mensajes")
class RedactorDeMensajesTest {

    private static final String ENLACE = "https://lupa.networkcom.com.ar/informe/abc123";

    private Auditoria auditoria() {
        Usuario usuario = Usuario.registrar("matias@networkcom.com.ar", "$2a$12$hash", "Matías");
        Auditoria auditoria = Auditoria.iniciar(usuario, "Barbería Don Ramón", "Barbería");
        auditoria.completarDatos("Resistencia, Chaco", "3624-556677", "Av. Sarmiento 1240", null);
        return auditoria;
    }

    /** Proveedor caído: obliga al redactor a usar la plantilla de respaldo. */
    private ProveedorIA proveedorCaido() {
        return new ProveedorIA() {
            @Override
            public String responder(String instrucciones, String contenido) {
                throw new ProveedorIAException("sin servicio");
            }

            @Override
            public String nombre() {
                return "caído";
            }
        };
    }

    private ProveedorIA proveedorQueEcoaLasInstrucciones() {
        return new ProveedorIA() {
            @Override
            public String responder(String instrucciones, String contenido) {
                return instrucciones;
            }

            @Override
            public String nombre() {
                return "eco";
            }
        };
    }

    @Test
    @DisplayName("la plantilla de WhatsApp se presenta, nombra el comercio y deja el enlace")
    void plantillaDeWhatsApp() {
        var redactor = new RedactorDeMensajes(proveedorCaido());
        var mensaje = redactor.redactar(auditoria(), CanalDeEnvio.WHATSAPP, ENLACE);

        assertThat(mensaje.redactadoPorIa()).isFalse();
        assertThat(mensaje.asunto()).isNull();
        assertThat(mensaje.cuerpo())
                .contains("Networkcom")
                .contains("Matías")
                .contains("Barbería Don Ramón")
                .contains(ENLACE);
    }

    @Test
    @DisplayName("la plantilla de correo trae asunto propio y el puntaje")
    void plantillaDeCorreo() {
        var redactor = new RedactorDeMensajes(proveedorCaido());
        var mensaje = redactor.redactar(auditoria(), CanalDeEnvio.EMAIL, ENLACE);

        assertThat(mensaje.asunto()).contains("Barbería Don Ramón");
        assertThat(mensaje.cuerpo()).contains("sobre 100").contains(ENLACE);
    }

    @Test
    @DisplayName("las plantillas no usan modismos que no van en un primer contacto comercial")
    void lasPlantillasMantienenElRegistro() {
        var redactor = new RedactorDeMensajes(proveedorCaido());

        for (CanalDeEnvio canal : CanalDeEnvio.values()) {
            String cuerpo = redactor.redactar(auditoria(), canal, ENLACE).cuerpo().toLowerCase();

            assertThat(RedactorDeMensajes.MODISMOS_PROHIBIDOS)
                    .as("registro del mensaje de %s", canal)
                    .allSatisfy(modismo -> assertThat(cuerpo).doesNotContain(modismo));
        }
    }

    @Test
    @DisplayName("al modelo se le pide el registro profesional y se le da el nombre de quien escribe")
    void lasInstruccionesPidenRegistroProfesional() {
        var redactor = new RedactorDeMensajes(proveedorQueEcoaLasInstrucciones());
        String instrucciones = redactor.redactar(auditoria(), CanalDeEnvio.WHATSAPP, ENLACE).cuerpo();

        assertThat(instrucciones)
                .contains("Matías")
                .contains("comercial y profesional")
                .contains("USTED")
                .contains("saludo cordial")
                .contains("Prohibido");
    }

    @Test
    @DisplayName("las plantillas abren con un saludo, no con la presentación")
    void lasPlantillasSaludanPrimero() {
        var redactor = new RedactorDeMensajes(proveedorCaido());

        for (CanalDeEnvio canal : CanalDeEnvio.values()) {
            String cuerpo = redactor.redactar(auditoria(), canal, ENLACE).cuerpo();

            assertThat(cuerpo)
                    .as("apertura del mensaje de %s", canal)
                    .startsWith("Buenos días.");
        }
    }

    @Test
    @DisplayName("las plantillas tratan de usted, no de vos")
    void lasPlantillasTratanDeUsted() {
        var redactor = new RedactorDeMensajes(proveedorCaido());

        for (CanalDeEnvio canal : CanalDeEnvio.values()) {
            String cuerpo = redactor.redactar(auditoria(), canal, ENLACE).cuerpo().toLowerCase();

            assertThat(cuerpo)
                    .as("tratamiento del mensaje de %s", canal)
                    .doesNotContain(" te dejo")
                    .doesNotContain("querés")
                    .doesNotContain("tu comercio")
                    .doesNotContain("te escribo");
        }
    }
}
