package com.networkcom.lupa.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Registro, login y acceso con token")
class AutenticacionIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    private String cuerpoRegistro(String email, String contrasena) {
        return """
                {"nombre":"Matias DT","email":"%s","contrasena":"%s"}
                """.formatted(email, contrasena);
    }

    private String cuerpoLogin(String email, String contrasena) {
        return """
                {"email":"%s","contrasena":"%s"}
                """.formatted(email, contrasena);
    }

    private MvcResult registrar(String email, String contrasena) throws Exception {
        return mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(email, contrasena)))
                .andReturn();
    }

    private String tokenDe(MvcResult resultado) throws Exception {
        JsonNode cuerpo = json.readTree(resultado.getResponse().getContentAsString());
        return cuerpo.get("token").asText();
    }

    @Test
    @DisplayName("el registro crea la cuenta y devuelve un token")
    void registroExitoso() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("dt@networkcom.com", "clave-segura-123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value("dt@networkcom.com"))
                .andExpect(jsonPath("$.usuario.nombre").value("Matias DT"));
    }

    @Test
    @DisplayName("la respuesta nunca incluye el hash de la contrasena")
    void nuncaFiltraElHash() throws Exception {
        MvcResult resultado = registrar("sinhash@networkcom.com", "clave-segura-123");
        String cuerpo = resultado.getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("contrasena");
        assertThat(cuerpo).doesNotContain("$2a$");
    }

    @Test
    @DisplayName("no se puede registrar dos veces el mismo email, ni cambiando mayusculas")
    void emailDuplicado() throws Exception {
        registrar("repetido@networkcom.com", "clave-segura-123");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("Repetido@Networkcom.com", "otra-clave-456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email ya registrado"));
    }

    @Test
    @DisplayName("una contrasena corta se rechaza con el detalle del campo")
    void contrasenaCorta() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("corta@networkcom.com", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contrasena").isNotEmpty());
    }

    @Test
    @DisplayName("el login devuelve token con las credenciales correctas")
    void loginExitoso() throws Exception {
        registrar("login@networkcom.com", "clave-segura-123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("login@networkcom.com", "clave-segura-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value("login@networkcom.com"));
    }

    @Test
    @DisplayName("la contrasena incorrecta y el email inexistente dan la misma respuesta")
    void credencialesInvalidasNoDistinguenElCaso() throws Exception {
        registrar("existe@networkcom.com", "clave-segura-123");

        String conClaveMala = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("existe@networkcom.com", "clave-equivocada")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String conEmailInexistente = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("nadie@networkcom.com", "clave-equivocada")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(conClaveMala).isEqualTo(conEmailInexistente);
    }

    @Test
    @DisplayName("sin token, un endpoint protegido devuelve 401 y no una redireccion")
    void sinTokenNoHayAcceso() throws Exception {
        mockMvc.perform(get("/api/auth/yo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("con un token invalido tampoco se entra")
    void tokenInvalido() throws Exception {
        mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer esto.no.es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("con el token del login se accede al perfil propio")
    void accesoConToken() throws Exception {
        String token = tokenDe(registrar("perfil@networkcom.com", "clave-segura-123"));

        mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("perfil@networkcom.com"))
                .andExpect(jsonPath("$.nombre").value("Matias DT"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }
}
