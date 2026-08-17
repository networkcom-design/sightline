package com.networkcom.lupa.application.ia;

import com.networkcom.lupa.domain.auditoria.Senal;

import java.util.EnumMap;
import java.util.Map;

/**
 * De dónde puede salir la respuesta de cada señal.
 *
 * Esta tabla es la que define cuánto trabajo se le saca de encima al auditor.
 * Si las dos evidencias están pegadas, quedan siete preguntas para contestar a
 * mano en lugar de treinta y cuatro.
 */
public enum FuenteDeEvidencia {

    /** La mide el analizador de sitios. No la contesta nadie. */
    SITIO,

    /** Sale del texto de la ficha de Google que el auditor pegó. */
    GOOGLE,

    /** Sale del texto del perfil de Instagram que el auditor pegó. */
    INSTAGRAM,

    /** Necesita cruzar varios canales: los datos declarados contra lo que dice cada perfil. */
    CRUZADA,

    /**
     * No hay forma de saberlo desde afuera.
     *
     * Si el comercio tiene mensaje de bienvenida configurado en WhatsApp, o si
     * responde los mensajes en el día, no está publicado en ningún lugar. No es
     * una limitación del modelo: el dato no existe públicamente.
     */
    SOLO_HUMANO;

    private static final Map<Senal, FuenteDeEvidencia> TABLA = construir();

    public static FuenteDeEvidencia de(Senal senal) {
        return TABLA.getOrDefault(senal, SOLO_HUMANO);
    }

    private static Map<Senal, FuenteDeEvidencia> construir() {
        Map<Senal, FuenteDeEvidencia> tabla = new EnumMap<>(Senal.class);

        Senal.automaticas().forEach(senal -> tabla.put(senal, SITIO));

        tabla.put(Senal.G_FICHA_EXISTE, GOOGLE);
        tabla.put(Senal.G_CATEGORIA, GOOGLE);
        tabla.put(Senal.G_HORARIOS, GOOGLE);
        tabla.put(Senal.G_FOTOS, GOOGLE);
        tabla.put(Senal.G_TELEFONO, GOOGLE);
        tabla.put(Senal.G_ENLACE, GOOGLE);
        tabla.put(Senal.G_DESCRIPCION, GOOGLE);
        tabla.put(Senal.G_RESPONDE_RESENAS, GOOGLE);
        tabla.put(Senal.REP_CANTIDAD, GOOGLE);
        tabla.put(Senal.REP_PROMEDIO, GOOGLE);
        tabla.put(Senal.REP_RECIENTES, GOOGLE);

        tabla.put(Senal.IG_EXISTE, INSTAGRAM);
        tabla.put(Senal.IG_BIO_CLARA, INSTAGRAM);
        tabla.put(Senal.IG_ENLACE, INSTAGRAM);
        tabla.put(Senal.IG_UBICACION, INSTAGRAM);
        tabla.put(Senal.IG_DESTACADOS, INSTAGRAM);
        tabla.put(Senal.IG_PROFESIONAL, INSTAGRAM);
        tabla.put(Senal.IG_FRECUENCIA, INSTAGRAM);
        tabla.put(Senal.IG_CTA, INSTAGRAM);
        tabla.put(Senal.CONT_PROPUESTA, INSTAGRAM);
        tabla.put(Senal.CONT_VARIEDAD, INSTAGRAM);
        tabla.put(Senal.CONT_PRECIOS, INSTAGRAM);
        tabla.put(Senal.CONT_PRUEBA_SOCIAL, INSTAGRAM);

        tabla.put(Senal.NAP_NOMBRE, CRUZADA);
        tabla.put(Senal.NAP_TELEFONO, CRUZADA);
        tabla.put(Senal.NAP_DIRECCION, CRUZADA);
        tabla.put(Senal.WA_ENLACE, CRUZADA);

        tabla.put(Senal.G_PUBLICACIONES, SOLO_HUMANO);
        tabla.put(Senal.REP_PIDE, SOLO_HUMANO);
        tabla.put(Senal.IG_RESPONDE, SOLO_HUMANO);
        tabla.put(Senal.WA_BUSINESS, SOLO_HUMANO);
        tabla.put(Senal.WA_BIENVENIDA, SOLO_HUMANO);
        tabla.put(Senal.WA_CATALOGO, SOLO_HUMANO);
        tabla.put(Senal.WA_AUSENCIA, SOLO_HUMANO);

        return tabla;
    }
}
