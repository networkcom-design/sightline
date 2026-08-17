package com.networkcom.lupa.domain.auditoria;

import java.util.Arrays;
import java.util.List;

import static com.networkcom.lupa.domain.auditoria.Dimension.CONSISTENCIA;
import static com.networkcom.lupa.domain.auditoria.Dimension.CONTENIDO;
import static com.networkcom.lupa.domain.auditoria.Dimension.GOOGLE;
import static com.networkcom.lupa.domain.auditoria.Dimension.INSTAGRAM;
import static com.networkcom.lupa.domain.auditoria.Dimension.REPUTACION;
import static com.networkcom.lupa.domain.auditoria.Dimension.SITIO_WEB;
import static com.networkcom.lupa.domain.auditoria.Dimension.WHATSAPP;
import static com.networkcom.lupa.domain.auditoria.Esfuerzo.ALTO;
import static com.networkcom.lupa.domain.auditoria.Esfuerzo.BAJO;
import static com.networkcom.lupa.domain.auditoria.Esfuerzo.MEDIO;

/**
 * Catálogo de señales: el conocimiento del producto.
 *
 * Cada señal es una cosa concreta que un comercio local puede tener bien o mal,
 * con su peso dentro de la dimensión y, si falta, qué hay que hacer al respecto.
 * Es acá donde vive la experiencia de la agencia, no en el prompt: la IA después
 * redacta e interpreta, pero qué se mide y cuánto pesa está decidido de antemano
 * y es siempre igual. Dos auditorías del mismo comercio dan el mismo puntaje.
 *
 * Las señales marcadas como automáticas las resuelve el analizador de sitios sin
 * intervención humana. El resto son las preguntas del cuestionario guiado.
 */
public enum Senal {

    // ---- Ficha de Google -------------------------------------------------

    G_FICHA_EXISTE(GOOGLE, 10, false,
            "¿El comercio aparece en Google Maps con una ficha propia?",
            "No tiene ficha en Google Maps",
            "Crear la ficha de Google Business Profile y verificarla. Es el canal por el que más gente busca un comercio local.",
            Impacto.ALTO, BAJO, 30),

    G_CATEGORIA(GOOGLE, 7, false,
            "¿La categoría principal describe exactamente el rubro?",
            "La categoría de la ficha no coincide con el rubro real",
            "Cambiar la categoría principal por la que describe el negocio. Google la usa para decidir en qué búsquedas aparece.",
            Impacto.ALTO, BAJO, 10),

    G_HORARIOS(GOOGLE, 6, false,
            "¿Los horarios están cargados y son los reales de hoy?",
            "Horarios ausentes o desactualizados",
            "Cargar los horarios reales y los feriados. Un cliente que llega y encuentra cerrado no vuelve, y además deja reseña negativa.",
            Impacto.ALTO, BAJO, 15),

    G_FOTOS(GOOGLE, 6, false,
            "¿Tiene al menos 10 fotos propias y de los últimos 6 meses?",
            "Pocas fotos o fotos viejas en la ficha",
            "Subir 10 a 15 fotos actuales: frente, interior, producto y equipo. Las fichas con fotos reciben más pedidos de dirección y llamadas.",
            Impacto.MEDIO, BAJO, 40),

    G_TELEFONO(GOOGLE, 5, false,
            "¿El teléfono de la ficha es el que realmente atiende?",
            "Teléfono ausente o incorrecto en la ficha",
            "Poner el número que se atiende, idealmente el mismo de WhatsApp.",
            Impacto.ALTO, BAJO, 5),

    G_ENLACE(GOOGLE, 5, false,
            "¿La ficha enlaza al sitio o directamente al WhatsApp?",
            "La ficha no lleva a ningún canal de contacto",
            "Agregar el sitio web o un link wa.me en el campo de sitio de la ficha.",
            Impacto.MEDIO, BAJO, 5),

    G_RESPONDE_RESENAS(GOOGLE, 6, false,
            "¿Responde las reseñas, tanto las buenas como las malas?",
            "Las reseñas quedan sin responder",
            "Responder todas las reseñas, empezando por las negativas. Una respuesta serena a una queja convence más que diez elogios.",
            Impacto.MEDIO, MEDIO, 60),

    G_DESCRIPCION(GOOGLE, 4, false,
            "¿La descripción explica qué vende y en qué zona?",
            "Descripción vacía o genérica",
            "Escribir una descripción de 300 caracteres con qué vende, para quién y la zona que cubre.",
            Impacto.BAJO, BAJO, 20),

    G_PUBLICACIONES(GOOGLE, 3, false,
            "¿Publica novedades u ofertas en la ficha?",
            "No usa las publicaciones de Google",
            "Publicar en la ficha una vez por semana. Es espacio gratis que casi nadie usa en el interior.",
            Impacto.BAJO, MEDIO, 30),

    // ---- Instagram -------------------------------------------------------

    IG_EXISTE(INSTAGRAM, 10, false,
            "¿Tiene una cuenta de Instagram activa?",
            "Sin cuenta de Instagram o abandonada",
            "Abrir la cuenta o retomarla. En comercio local es la vidriera principal.",
            Impacto.ALTO, MEDIO, 60),

    IG_BIO_CLARA(INSTAGRAM, 8, false,
            "¿La bio dice en una línea qué vende y a quién?",
            "La bio no explica qué vende el negocio",
            "Reescribir la bio con la fórmula: qué vende, para quién, dónde queda y qué hacer ahora.",
            Impacto.ALTO, BAJO, 15),

    IG_FRECUENCIA(INSTAGRAM, 8, false,
            "¿Publicó al menos 4 veces en el último mes?",
            "Publica menos de una vez por semana",
            "Sostener un mínimo de una publicación semanal. La constancia importa más que la producción.",
            Impacto.ALTO, ALTO, 120),

    IG_ENLACE(INSTAGRAM, 7, false,
            "¿Tiene link a WhatsApp o al sitio en la bio?",
            "La bio no tiene link de contacto",
            "Poner un link wa.me con mensaje prellenado en la bio.",
            Impacto.ALTO, BAJO, 10),

    IG_CTA(INSTAGRAM, 6, false,
            "¿Los textos de los posts invitan a hacer algo?",
            "Los posts no piden ninguna acción",
            "Cerrar cada post con una acción concreta: escribinos, reservá, pasá por el local.",
            Impacto.MEDIO, BAJO, 20),

    IG_RESPONDE(INSTAGRAM, 6, false,
            "¿Responde comentarios y mensajes en el día?",
            "Mensajes y comentarios sin responder",
            "Fijar dos momentos del día para responder. Un mensaje sin respuesta a las 24 horas es una venta perdida.",
            Impacto.ALTO, MEDIO, 30),

    IG_UBICACION(INSTAGRAM, 5, false,
            "¿Queda claro dónde queda el local?",
            "No se ve la ubicación en el perfil",
            "Poner la dirección en la bio y etiquetar la ubicación en cada post.",
            Impacto.MEDIO, BAJO, 10),

    IG_DESTACADOS(INSTAGRAM, 5, false,
            "¿Tiene historias destacadas organizadas por tema?",
            "Sin historias destacadas",
            "Armar destacados de precios, cómo llegar, opiniones y preguntas frecuentes.",
            Impacto.MEDIO, MEDIO, 45),

    IG_PROFESIONAL(INSTAGRAM, 4, false,
            "¿Es una cuenta profesional o de empresa?",
            "Usa cuenta personal en lugar de profesional",
            "Convertir a cuenta profesional: habilita estadísticas y botones de contacto.",
            Impacto.MEDIO, BAJO, 5),

    // ---- Sitio web: todas automáticas -------------------------------------

    WEB_EXISTE(SITIO_WEB, 10, true,
            "¿Tiene sitio web?",
            "No tiene sitio web",
            "Armar aunque sea una página única con qué vende, dónde queda, horarios y botón de WhatsApp.",
            Impacto.MEDIO, ALTO, 480),

    WEB_RESPONDE(SITIO_WEB, 8, true,
            "¿El sitio responde correctamente?",
            "El sitio no responde o devuelve error",
            "Revisar el hosting y el dominio. Un sitio caído es peor que no tener sitio.",
            Impacto.ALTO, MEDIO, 60),

    WEB_HTTPS(SITIO_WEB, 7, true,
            "¿El sitio usa HTTPS?",
            "El sitio no tiene certificado seguro",
            "Activar el certificado. El navegador muestra 'no seguro' y espanta clientes.",
            Impacto.ALTO, BAJO, 20),

    WEB_MOBILE(SITIO_WEB, 8, true,
            "¿Está preparado para celulares?",
            "El sitio no está adaptado a celulares",
            "Agregar la etiqueta viewport y revisar el diseño en móvil. La mayoría del tráfico local entra desde el teléfono.",
            Impacto.ALTO, MEDIO, 120),

    WEB_TITULO(SITIO_WEB, 6, true,
            "¿Tiene un título descriptivo para buscadores?",
            "Título ausente, genérico o demasiado largo",
            "Escribir un título de 50 a 60 caracteres con rubro y ciudad.",
            Impacto.MEDIO, BAJO, 10),

    WEB_META(SITIO_WEB, 5, true,
            "¿Tiene descripción para buscadores?",
            "Sin meta descripción",
            "Escribir una descripción de 150 caracteres: es el texto que Google muestra debajo del título.",
            Impacto.BAJO, BAJO, 10),

    WEB_H1(SITIO_WEB, 4, true,
            "¿Tiene un encabezado principal claro?",
            "Sin encabezado H1 o con varios",
            "Dejar un solo H1 que diga qué es el negocio.",
            Impacto.BAJO, BAJO, 10),

    WEB_VELOCIDAD(SITIO_WEB, 6, true,
            "¿Carga en menos de 2,5 segundos?",
            "El sitio tarda demasiado en cargar",
            "Comprimir las imágenes, que suelen ser el 80% del peso.",
            Impacto.MEDIO, MEDIO, 90),

    WEB_CONTACTO(SITIO_WEB, 6, true,
            "¿El contacto está visible sin tener que buscarlo?",
            "El contacto no aparece en la página principal",
            "Poner teléfono y WhatsApp arriba de todo y repetirlos al pie.",
            Impacto.ALTO, BAJO, 20),

    // ---- Reputación -------------------------------------------------------

    REP_CANTIDAD(REPUTACION, 10, false,
            "¿Tiene 20 reseñas o más en Google?",
            "Pocas reseñas para competir",
            "Pedir reseñas sistemáticamente: un cartel con QR en el mostrador y un mensaje después de cada compra.",
            Impacto.ALTO, MEDIO, 60),

    REP_PROMEDIO(REPUTACION, 10, false,
            "¿El promedio es de 4,0 o más?",
            "Promedio de reseñas por debajo de 4",
            "Atacar la causa de las quejas repetidas antes de pedir más reseñas, o solo se acumulan malas.",
            Impacto.ALTO, ALTO, 240),

    REP_RECIENTES(REPUTACION, 6, false,
            "¿Tiene reseñas de los últimos 3 meses?",
            "Las reseñas son todas viejas",
            "Retomar el pedido de reseñas. Una ficha sin movimiento parece un negocio cerrado.",
            Impacto.MEDIO, BAJO, 30),

    REP_PIDE(REPUTACION, 4, false,
            "¿Pide reseñas de forma sistemática?",
            "No hay un proceso para pedir reseñas",
            "Definir el momento exacto en que se pide y quién lo hace.",
            Impacto.MEDIO, BAJO, 30),

    // ---- WhatsApp ---------------------------------------------------------

    WA_BUSINESS(WHATSAPP, 8, false,
            "¿Usa WhatsApp Business y no el personal?",
            "Atiende con WhatsApp personal",
            "Migrar a WhatsApp Business: habilita catálogo, respuestas rápidas y horarios.",
            Impacto.ALTO, BAJO, 30),

    WA_BIENVENIDA(WHATSAPP, 5, false,
            "¿Tiene mensaje de bienvenida configurado?",
            "Sin mensaje de bienvenida",
            "Configurar un saludo que diga horarios y en cuánto responden.",
            Impacto.MEDIO, BAJO, 10),

    WA_CATALOGO(WHATSAPP, 5, false,
            "¿Tiene el catálogo cargado con precios?",
            "Catálogo vacío o sin precios",
            "Cargar los 10 productos o servicios más vendidos con precio. Evita la pregunta '¿cuánto sale?' cien veces por semana.",
            Impacto.ALTO, MEDIO, 90),

    WA_AUSENCIA(WHATSAPP, 4, false,
            "¿Tiene mensaje de ausencia fuera de horario?",
            "Sin mensaje de ausencia",
            "Configurar el mensaje fuera de hora para que nadie quede esperando.",
            Impacto.BAJO, BAJO, 10),

    WA_ENLACE(WHATSAPP, 4, false,
            "¿Usa links wa.me en sus perfiles?",
            "No hay links directos a WhatsApp",
            "Generar un link wa.me con mensaje prellenado y ponerlo en Instagram, Google y el sitio.",
            Impacto.MEDIO, BAJO, 15),

    // ---- Consistencia -----------------------------------------------------

    NAP_NOMBRE(CONSISTENCIA, 8, false,
            "¿El nombre del comercio es idéntico en Google, Instagram y el sitio?",
            "El nombre cambia según el canal",
            "Unificar el nombre exacto en todos lados, incluidas mayúsculas y abreviaturas.",
            Impacto.MEDIO, BAJO, 20),

    NAP_TELEFONO(CONSISTENCIA, 7, false,
            "¿El teléfono es el mismo en todos los canales?",
            "Hay más de un teléfono dando vueltas",
            "Dejar un único número de contacto público.",
            Impacto.MEDIO, BAJO, 15),

    NAP_DIRECCION(CONSISTENCIA, 6, false,
            "¿La dirección coincide en todos lados?",
            "La dirección no coincide entre canales",
            "Unificar la dirección con el mismo formato en Google, Instagram y el sitio.",
            Impacto.MEDIO, BAJO, 15),

    // ---- Contenido --------------------------------------------------------

    CONT_PROPUESTA(CONTENIDO, 8, false,
            "¿Un desconocido entiende qué vende en 5 segundos?",
            "No se entiende rápido qué vende el negocio",
            "Definir una frase de propuesta y repetirla en la bio, el sitio y la ficha.",
            Impacto.ALTO, MEDIO, 60),

    CONT_PRECIOS(CONTENIDO, 5, false,
            "¿Muestra precios o al menos un rango?",
            "No muestra precios en ningún lado",
            "Publicar precios o rangos. Ocultarlos no genera consultas, genera abandono.",
            Impacto.MEDIO, BAJO, 30),

    CONT_PRUEBA_SOCIAL(CONTENIDO, 5, false,
            "¿Muestra clientes reales, testimonios o trabajos hechos?",
            "Sin prueba social visible",
            "Publicar un testimonio o un trabajo real por semana.",
            Impacto.MEDIO, MEDIO, 45),

    CONT_VARIEDAD(CONTENIDO, 4, false,
            "¿Varía entre producto, prueba social y detrás de escena?",
            "Publica siempre el mismo tipo de contenido",
            "Rotar tres formatos: producto, cliente y detrás de escena.",
            Impacto.BAJO, MEDIO, 45);

    private final Dimension dimension;
    private final int peso;
    private final boolean automatica;
    private final String pregunta;
    private final String hallazgo;
    private final String accion;
    private final Impacto impacto;
    private final Esfuerzo esfuerzo;
    private final int minutosEstimados;

    Senal(Dimension dimension, int peso, boolean automatica, String pregunta, String hallazgo,
          String accion, Impacto impacto, Esfuerzo esfuerzo, int minutosEstimados) {
        this.dimension = dimension;
        this.peso = peso;
        this.automatica = automatica;
        this.pregunta = pregunta;
        this.hallazgo = hallazgo;
        this.accion = accion;
        this.impacto = impacto;
        this.esfuerzo = esfuerzo;
        this.minutosEstimados = minutosEstimados;
    }

    public static List<Senal> de(Dimension dimension) {
        return Arrays.stream(values()).filter(senal -> senal.dimension == dimension).toList();
    }

    /** Las que contesta el auditor mirando los perfiles. */
    public static List<Senal> manuales() {
        return Arrays.stream(values()).filter(senal -> !senal.automatica).toList();
    }

    /** Las que resuelve el analizador de sitios sin intervención. */
    public static List<Senal> automaticas() {
        return Arrays.stream(values()).filter(Senal::isAutomatica).toList();
    }

    public Dimension getDimension() {
        return dimension;
    }

    public int getPeso() {
        return peso;
    }

    public boolean isAutomatica() {
        return automatica;
    }

    public String getPregunta() {
        return pregunta;
    }

    public String getHallazgo() {
        return hallazgo;
    }

    public String getAccion() {
        return accion;
    }

    public Impacto getImpacto() {
        return impacto;
    }

    public Esfuerzo getEsfuerzo() {
        return esfuerzo;
    }

    public int getMinutosEstimados() {
        return minutosEstimados;
    }

    /**
     * Prioridad para ordenar el plan de acción: primero lo que más mueve la
     * aguja con menos trabajo. Un arreglo de diez minutos con impacto alto tiene
     * que aparecer antes que un rediseño de sitio, aunque el sitio también importe.
     */
    public double prioridad() {
        return (double) impacto.getValor() / esfuerzo.getValor();
    }
}
