package com.networkcom.lupa.domain.auditoria;

import com.networkcom.lupa.domain.propuesta.AjusteServicio;
import com.networkcom.lupa.domain.usuario.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Una auditoría de un comercio: la raíz del agregado.
 *
 * Guarda tanto el resultado como la evidencia cruda que lo produjo. Eso último
 * no es sentimentalismo: si un dictamen se discute, hay que poder mostrar de
 * dónde salió, y si mejoramos las instrucciones de la IA, hay que poder
 * reanalizar sin volver a abrir los perfiles.
 */
@Entity
@Table(name = "auditorias")
public class Auditoria {

    private static final SecureRandom AZAR = new SecureRandom();

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String rubro;

    @Column(length = 120)
    private String ciudad;

    @Column(length = 60)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    @Column(name = "texto_instagram", columnDefinition = "TEXT")
    private String textoInstagram;

    @Column(name = "texto_google", columnDefinition = "TEXT")
    private String textoGoogle;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Convert(converter = ConversorMedicionSitio.class)
    @Column(columnDefinition = "TEXT")
    private MedicionSitio medicion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoAuditoria estado;

    @Column(name = "token_publico", nullable = false, length = 64)
    private String tokenPublico;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Convert(converter = ConversorAjustesPropuesta.class)
    @Column(name = "ajustes_propuesta", columnDefinition = "TEXT")
    private Map<String, AjusteServicio> ajustesPropuesta = new LinkedHashMap<>();

    @OneToMany(mappedBy = "auditoria", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RespuestaSenal> respuestas = new ArrayList<>();

    protected Auditoria() {
    }

    private Auditoria(Usuario usuario, String nombre, String rubro) {
        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.nombre = nombre.trim();
        this.rubro = rubro.trim();
        this.estado = EstadoAuditoria.BORRADOR;
        this.tokenPublico = generarToken();
        this.creadaEn = Instant.now();
        this.actualizadaEn = this.creadaEn;
    }

    public static Auditoria iniciar(Usuario usuario, String nombre, String rubro) {
        return new Auditoria(usuario, nombre, rubro);
    }

    /**
     * 32 bytes de azar criptográfico.
     *
     * El token es la única credencial del enlace público: quien lo tenga, ve el
     * informe. Un identificador secuencial o un UUID basado en tiempo permitiría
     * adivinar informes de otros comercios probando valores cercanos.
     */
    private static String generarToken() {
        byte[] bytes = new byte[24];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void completarDatos(String ciudad, String telefono, String direccion, String sitioWeb) {
        this.ciudad = ciudad;
        this.telefono = telefono;
        this.direccion = direccion;
        this.sitioWeb = sitioWeb;
        tocar();
    }

    public void registrarMedicion(MedicionSitio medicion, Map<Senal, EstadoSenal> senalesMedidas) {
        this.medicion = medicion;
        senalesMedidas.forEach((senal, estado) ->
                responder(Dictamen.medido(senal, estado, explicarMedicion(senal, medicion))));
        tocar();
    }

    public void guardarEvidencia(String textoInstagram, String textoGoogle, String notas) {
        this.textoInstagram = textoInstagram;
        this.textoGoogle = textoGoogle;
        this.notas = notas;
        tocar();
    }

    public void registrarDictamenes(Map<Senal, Dictamen> dictamenes) {
        dictamenes.values().forEach(this::responder);
        if (!dictamenes.isEmpty() && estado == EstadoAuditoria.BORRADOR) {
            this.estado = EstadoAuditoria.ANALIZADA;
        }
        tocar();
    }

    /** Corrección del auditor. Es la última palabra sobre esa señal. */
    public void corregir(Senal senal, EstadoSenal estado) {
        responder(Dictamen.humano(senal, estado));
        tocar();
    }

    /**
     * Cambia si un servicio entra en el presupuesto y a qué precio.
     *
     * `precio` en nulo vuelve al valor de lista. Los ajustes son de esta
     * auditoría: no tocan el catálogo ni las propuestas ya armadas para otros
     * comercios.
     */
    public void ajustarServicio(String codigoServicio, boolean incluido, BigDecimal precio) {
        if (precio != null && precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }

        AjusteServicio actual = ajustesPropuesta.getOrDefault(codigoServicio, AjusteServicio.porDefecto());
        ajustesPropuesta.put(codigoServicio, new AjusteServicio(incluido, precio));

        // Se reasigna el mapa para que Hibernate detecte el cambio: el conversor
        // trabaja sobre el valor de la columna, y mutar el mapa en el lugar no
        // siempre marca la entidad como sucia.
        this.ajustesPropuesta = new LinkedHashMap<>(ajustesPropuesta);

        if (actual.incluido() != incluido || !java.util.Objects.equals(actual.precio(), precio)) {
            tocar();
        }
    }

    public Map<String, AjusteServicio> getAjustesPropuesta() {
        return Map.copyOf(ajustesPropuesta);
    }

    public void marcarRevisada() {
        this.estado = EstadoAuditoria.REVISADA;
        tocar();
    }

    public void marcarEnviada() {
        this.estado = EstadoAuditoria.ENVIADA;
        tocar();
    }

    private void responder(Dictamen dictamen) {
        respuestas.stream()
                .filter(respuesta -> respuesta.getSenal() == dictamen.senal())
                .findFirst()
                .ifPresentOrElse(
                        respuesta -> respuesta.aplicar(dictamen),
                        () -> respuestas.add(new RespuestaSenal(this, dictamen)));
    }

    /** Las respuestas en el formato que espera el motor de puntaje. */
    public Map<Senal, EstadoSenal> respuestasParaElMotor() {
        Map<Senal, EstadoSenal> mapa = new EnumMap<>(Senal.class);
        respuestas.forEach(respuesta -> mapa.put(respuesta.getSenal(), respuesta.getEstado()));
        return mapa;
    }

    public Map<Senal, Dictamen> dictamenes() {
        Map<Senal, Dictamen> mapa = new EnumMap<>(Senal.class);
        respuestas.forEach(respuesta -> mapa.put(respuesta.getSenal(), respuesta.aDictamen()));
        return mapa;
    }

    /** Las que la IA respondió con dudas. Son las que hay que mirar sí o sí. */
    public List<RespuestaSenal> paraRevisar() {
        return respuestas.stream().filter(respuesta -> respuesta.aDictamen().necesitaRevision()).toList();
    }

    /** Las que todavía no contestó nadie. */
    public List<Senal> sinResponder() {
        List<Senal> contestadas = respuestas.stream().map(RespuestaSenal::getSenal).toList();
        return List.of(Senal.values()).stream().filter(senal -> !contestadas.contains(senal)).toList();
    }

    public boolean perteneceA(Usuario otro) {
        return usuario != null && usuario.getId().equals(otro.getId());
    }

    private static String explicarMedicion(Senal senal, MedicionSitio medicion) {
        if (medicion == null || !medicion.alcanzable()) {
            return medicion == null ? null : medicion.error();
        }
        return switch (senal) {
            case WEB_VELOCIDAD -> "Respondió en " + medicion.segundos() + " s.";
            case WEB_TITULO -> medicion.titulo() == null || medicion.titulo().isBlank()
                    ? "El sitio no tiene título."
                    : "El título tiene " + medicion.titulo().length() + " caracteres.";
            case WEB_META -> medicion.metaDescripcion() == null
                    ? "No se encontró meta descripción."
                    : "La descripción tiene " + medicion.metaDescripcion().length() + " caracteres.";
            case WEB_H1 -> "Se encontraron " + medicion.cantidadH1() + " encabezados H1.";
            case WEB_MOBILE -> medicion.tieneViewport()
                    ? "Tiene la etiqueta viewport."
                    : "No se encontró la etiqueta viewport en el HTML.";
            case WEB_HTTPS -> medicion.esHttps() ? "Responde por HTTPS." : "No usa HTTPS.";
            case WEB_CONTACTO -> medicion.tieneContactoVisible()
                    ? "Hay enlaces de contacto directo en la página."
                    : "No se encontraron enlaces tel:, mailto: ni wa.me.";
            case WEB_RESPONDE -> "El sitio respondió con código " + medicion.codigoHttp() + ".";
            case WEB_EXISTE -> "Se declaró un sitio web.";
            default -> null;
        };
    }

    private void tocar() {
        this.actualizadaEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRubro() {
        return rubro;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public String getTextoInstagram() {
        return textoInstagram;
    }

    public String getTextoGoogle() {
        return textoGoogle;
    }

    public String getNotas() {
        return notas;
    }

    public MedicionSitio getMedicion() {
        return medicion;
    }

    public EstadoAuditoria getEstado() {
        return estado;
    }

    public String getTokenPublico() {
        return tokenPublico;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }

    public List<RespuestaSenal> getRespuestas() {
        return List.copyOf(respuestas);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Auditoria auditoria)) {
            return false;
        }
        return Objects.equals(id, auditoria.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
