package com.networkcom.lupa.application.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networkcom.lupa.domain.auditoria.Dictamen;
import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.OrigenDeRespuesta;
import com.networkcom.lupa.domain.auditoria.Senal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Analista de evidencia")
class AnalistaDeEvidenciaTest {

    private final ObjectMapper json = new ObjectMapper();

    /** Proveedor falso: devuelve lo que se le indique, sin gastar cuota real. */
    private ProveedorIA proveedorQueDevuelve(String respuesta) {
        return new ProveedorIA() {
            @Override
            public String responder(String instrucciones, String contenido) {
                return respuesta;
            }

            @Override
            public String nombre() {
                return "falso";
            }
        };
    }

    private ProveedorIA proveedorQueFalla() {
        return new ProveedorIA() {
            @Override
            public String responder(String instrucciones, String contenido) {
                throw new ProveedorIAException("se cayó el servicio");
            }

            @Override
            public String nombre() {
                return "roto";
            }
        };
    }

    private Evidencia evidenciaCompleta() {
        return new Evidencia(
                "Barbería Don Ramón", "Barbería", "Resistencia, Chaco",
                "3624-556677", "Av. Sarmiento 1240", "donramonbarber.com.ar",
                null,
                "barberiadonramon · 1.240 seguidores · 87 publicaciones · Cortes y barba",
                "Barbería Don Ramón · 4,6 estrellas · 63 reseñas · Abierto ahora",
                null);
    }

    private Evidencia evidenciaVacia() {
        return new Evidencia("Kiosco", "Kiosco", "Resistencia", null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("con las dos evidencias pegadas, quedan solo 7 preguntas para contestar a mano")
    void reduceElCuestionarioA7() {
        var evaluables = AnalistaDeEvidencia.senalesEvaluables(evidenciaCompleta());

        long soloHumano = List.of(Senal.values()).stream()
                .filter(senal -> FuenteDeEvidencia.de(senal) == FuenteDeEvidencia.SOLO_HUMANO)
                .count();

        assertThat(evaluables).hasSize(27);
        assertThat(soloHumano).isEqualTo(7);
        assertThat(evaluables.size() + soloHumano + Senal.automaticas().size())
                .isEqualTo(Senal.values().length);
    }

    @Test
    @DisplayName("sin evidencia pegada no se le pregunta nada a la IA")
    void sinEvidenciaNoHayAnalisis() {
        assertThat(AnalistaDeEvidencia.senalesEvaluables(evidenciaVacia())).isEmpty();
    }

    @Test
    @DisplayName("interpreta la respuesta aunque venga envuelta en un bloque de código")
    void toleraElFormatoDeLosModelos() {
        String respuesta = """
                Claro, acá va el análisis:
                ```json
                [
                  {"codigo":"REP_PROMEDIO","estado":"CUMPLE","confianza":"ALTA","fundamento":"La ficha muestra 4,6 estrellas."},
                  {"codigo":"REP_CANTIDAD","estado":"CUMPLE","confianza":"ALTA","fundamento":"Tiene 63 reseñas."}
                ]
                ```
                Espero que te sirva.
                """;

        var analista = new AnalistaDeEvidencia(proveedorQueDevuelve(respuesta), json);
        var dictamenes = analista.analizar(evidenciaCompleta()).dictamenes();

        assertThat(dictamenes).hasSize(2);
        assertThat(dictamenes.get(Senal.REP_PROMEDIO).estado()).isEqualTo(EstadoSenal.CUMPLE);
        assertThat(dictamenes.get(Senal.REP_PROMEDIO).origen()).isEqualTo(OrigenDeRespuesta.IA);
        assertThat(dictamenes.get(Senal.REP_CANTIDAD).fundamento()).contains("63");
    }

    @Test
    @DisplayName("descarta señales que no se preguntaron")
    void noDejaQueElModeloElijaQueSeAudita() {
        String respuesta = """
                [
                  {"codigo":"REP_PROMEDIO","estado":"CUMPLE","confianza":"ALTA","fundamento":"4,6 estrellas."},
                  {"codigo":"WA_CATALOGO","estado":"CUMPLE","confianza":"ALTA","fundamento":"Me parece que sí."},
                  {"codigo":"SENAL_INVENTADA","estado":"CUMPLE","confianza":"ALTA","fundamento":"..."}
                ]
                """;

        var analista = new AnalistaDeEvidencia(proveedorQueDevuelve(respuesta), json);
        var dictamenes = analista.analizar(evidenciaCompleta()).dictamenes();

        assertThat(dictamenes).containsOnlyKeys(Senal.REP_PROMEDIO);
    }

    @Test
    @DisplayName("un estado que no existe se descarta en vez de romper todo el análisis")
    void ignoraEstadosInvalidos() {
        String respuesta = """
                [
                  {"codigo":"REP_PROMEDIO","estado":"MAS_O_MENOS","confianza":"ALTA","fundamento":"Ni fu ni fa."},
                  {"codigo":"REP_CANTIDAD","estado":"CUMPLE","confianza":"ALTA","fundamento":"63 reseñas."}
                ]
                """;

        var analista = new AnalistaDeEvidencia(proveedorQueDevuelve(respuesta), json);

        assertThat(analista.analizar(evidenciaCompleta()).dictamenes()).containsOnlyKeys(Senal.REP_CANTIDAD);
    }

    @Test
    @DisplayName("una confianza desconocida se toma como baja, para que se revise")
    void anteLaDudaMarcaParaRevisar() {
        String respuesta = """
                [{"codigo":"IG_BIO_CLARA","estado":"NO_CUMPLE","confianza":"regular","fundamento":"La bio dice solo 'Cortes y barba'."}]
                """;

        var analista = new AnalistaDeEvidencia(proveedorQueDevuelve(respuesta), json);
        var dictamen = analista.analizar(evidenciaCompleta()).dictamenes().get(Senal.IG_BIO_CLARA);

        assertThat(dictamen.confianza()).isEqualTo(Dictamen.Confianza.BAJA);
        assertThat(dictamen.necesitaRevision()).isTrue();
    }

    @Test
    @DisplayName("si el proveedor se cae, las señales caen al cuestionario en vez de inventarse")
    void anteFalloNoInventa() {
        var analista = new AnalistaDeEvidencia(proveedorQueFalla(), json);

        assertThat(analista.analizar(evidenciaCompleta()).dictamenes()).isEmpty();
    }

    @Test
    @DisplayName("si la respuesta no es JSON, tampoco se inventa nada")
    void anteBasuraNoInventa() {
        var analista = new AnalistaDeEvidencia(
                proveedorQueDevuelve("No puedo ayudarte con eso."), json);

        assertThat(analista.analizar(evidenciaCompleta()).dictamenes()).isEmpty();
    }

    @Test
    @DisplayName("un fundamento larguísimo se recorta en vez de romper la pantalla")
    void recortaFundamentosLargos() {
        String largo = "x".repeat(600);
        var analista = new AnalistaDeEvidencia(proveedorQueDevuelve(
                "[{\"codigo\":\"REP_CANTIDAD\",\"estado\":\"CUMPLE\",\"confianza\":\"ALTA\",\"fundamento\":\"" + largo + "\"}]"),
                json);

        var dictamen = analista.analizar(evidenciaCompleta()).dictamenes().get(Senal.REP_CANTIDAD);

        assertThat(dictamen.fundamento()).hasSize(240).endsWith("...");
    }
}
