package com.networkcom.lupa.domain.propuesta;

import com.networkcom.lupa.domain.auditoria.Senal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.networkcom.lupa.domain.propuesta.Servicio.Modalidad.MENSUAL;
import static com.networkcom.lupa.domain.propuesta.Servicio.Modalidad.UNICO;

/**
 * Los servicios que vende Networkcom.
 *
 * Los precios son de referencia: están para que la herramienta funcione desde
 * el primer día, no para fijar la política comercial. Se editan desde la
 * aplicación y quedan guardados por usuario.
 *
 * Entre los nueve servicios cubren las 43 señales, lo cual se verifica por
 * test: si mañana se agrega una señal y ningún servicio la resuelve, la
 * auditoría detectaría un problema que la agencia no sabe cómo cobrar.
 */
public final class CatalogoServicios {

    private CatalogoServicios() {
    }

    private static Servicio servicio(String codigo, String nombre, String descripcion,
                                     Set<Senal> senales, long precio,
                                     Servicio.Modalidad modalidad, int plazoDias) {
        return new Servicio(codigo, nombre, descripcion, senales,
                BigDecimal.valueOf(precio), modalidad, plazoDias);
    }

    public static final Servicio FICHA_GOOGLE = servicio(
            "ficha-google",
            "Puesta a punto de la ficha de Google",
            "Creación o corrección de la ficha completa: categoría, horarios, fotos, descripción, "
                    + "teléfono y enlaces. Es el canal por el que más gente busca un comercio local.",
            Set.of(Senal.G_FICHA_EXISTE, Senal.G_CATEGORIA, Senal.G_HORARIOS, Senal.G_FOTOS,
                    Senal.G_TELEFONO, Senal.G_ENLACE, Senal.G_DESCRIPCION, Senal.G_PUBLICACIONES),
            85_000, UNICO, 7);

    public static final Servicio GESTION_RESENAS = servicio(
            "gestion-resenas",
            "Gestión de reseñas",
            "Respuesta a todas las reseñas y un circuito para pedirlas de forma sistemática, "
                    + "para que el promedio suba y se mantenga vivo.",
            Set.of(Senal.G_RESPONDE_RESENAS, Senal.REP_CANTIDAD, Senal.REP_PROMEDIO,
                    Senal.REP_RECIENTES, Senal.REP_PIDE),
            45_000, MENSUAL, 30);

    public static final Servicio KIT_INSTAGRAM = servicio(
            "kit-instagram",
            "Kit de Instagram",
            "Puesta a punto del perfil: bio con propuesta clara, link de contacto, ubicación, "
                    + "historias destacadas y conversión a cuenta profesional.",
            Set.of(Senal.IG_EXISTE, Senal.IG_BIO_CLARA, Senal.IG_ENLACE, Senal.IG_UBICACION,
                    Senal.IG_DESTACADOS, Senal.IG_PROFESIONAL),
            120_000, UNICO, 10);

    public static final Servicio CONTENIDO_MENSUAL = servicio(
            "contenido-mensual",
            "Contenido mensual",
            "Producción y publicación de contenido con calendario propio: producto, prueba social "
                    + "y detrás de escena, con llamada a la acción en cada pieza.",
            Set.of(Senal.IG_FRECUENCIA, Senal.IG_CTA, Senal.CONT_PROPUESTA, Senal.CONT_VARIEDAD,
                    Senal.CONT_PRECIOS, Senal.CONT_PRUEBA_SOCIAL),
            180_000, MENSUAL, 30);

    public static final Servicio ATENCION_MENSAJES = servicio(
            "atencion-mensajes",
            "Atención de mensajes",
            "Respuesta de comentarios y mensajes directos dentro del día, que es donde se pierde "
                    + "la mayoría de las ventas de un comercio local.",
            Set.of(Senal.IG_RESPONDE),
            90_000, MENSUAL, 30);

    public static final Servicio SITIO_UNA_PAGINA = servicio(
            "sitio-una-pagina",
            "Sitio de una página",
            "Sitio propio con dominio, certificado seguro, diseño para celulares y contacto "
                    + "visible arriba de todo.",
            Set.of(Senal.WEB_EXISTE, Senal.WEB_RESPONDE, Senal.WEB_HTTPS, Senal.WEB_MOBILE,
                    Senal.WEB_CONTACTO),
            450_000, UNICO, 21);

    public static final Servicio SEO_BASICO = servicio(
            "seo-basico",
            "Optimización para buscadores",
            "Títulos, descripciones, encabezados y compresión de imágenes para que el sitio "
                    + "cargue rápido y Google entienda de qué se trata.",
            Set.of(Senal.WEB_TITULO, Senal.WEB_META, Senal.WEB_H1, Senal.WEB_VELOCIDAD),
            95_000, UNICO, 7);

    public static final Servicio WHATSAPP_BUSINESS = servicio(
            "whatsapp-business",
            "WhatsApp Business configurado",
            "Migración a WhatsApp Business con catálogo cargado, mensajes de bienvenida y "
                    + "ausencia, y enlaces directos en todos los perfiles.",
            Set.of(Senal.WA_BUSINESS, Senal.WA_BIENVENIDA, Senal.WA_CATALOGO, Senal.WA_AUSENCIA,
                    Senal.WA_ENLACE),
            60_000, UNICO, 5);

    public static final Servicio UNIFICACION_DATOS = servicio(
            "unificacion-datos",
            "Unificación de datos",
            "Mismo nombre, misma dirección y mismo teléfono en todos los canales. Es el error "
                    + "silencioso que más posiciones cuesta en las búsquedas locales.",
            Set.of(Senal.NAP_NOMBRE, Senal.NAP_TELEFONO, Senal.NAP_DIRECCION),
            40_000, UNICO, 5);

    public static final List<Servicio> TODOS = List.of(
            FICHA_GOOGLE,
            GESTION_RESENAS,
            KIT_INSTAGRAM,
            CONTENIDO_MENSUAL,
            ATENCION_MENSAJES,
            SITIO_UNA_PAGINA,
            SEO_BASICO,
            WHATSAPP_BUSINESS,
            UNIFICACION_DATOS);

    /** Todas las señales que el catálogo sabe resolver. */
    public static Set<Senal> senalesCubiertas() {
        return TODOS.stream()
                .flatMap(servicio -> servicio.senalesQueResuelve().stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
