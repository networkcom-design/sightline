package com.networkcom.lupa.domain.contacto;

import java.util.Optional;

/**
 * Convierte un teléfono argentino escrito como sea al formato que pide wa.me.
 *
 * Es más engorroso de lo que parece. La gente anota los números de seis maneras
 * distintas —"3624-556677", "0362 15-4556677", "+54 9 362 455-6677"— y wa.me
 * necesita exactamente `549` seguido del código de área y el abonado, sin el 0
 * de larga distancia y sin el 15 de celular.
 *
 * Si el número no se puede normalizar con confianza se devuelve vacío. Abrir
 * WhatsApp con un número mal armado es peor que no abrirlo: el mensaje se manda
 * a cualquier lado o el chat aparece vacío y el auditor no se entera.
 */
public final class TelefonoArgentino {

    /** Código de área más abonado, sin prefijos: siempre 10 dígitos en Argentina. */
    private static final int DIGITOS_NACIONALES = 10;

    private static final String PAIS = "54";
    private static final String MOVIL = "9";

    private TelefonoArgentino() {
    }

    /**
     * Devuelve el número listo para wa.me, o vacío si no se pudo interpretar.
     */
    public static Optional<String> paraWhatsApp(String crudo) {
        if (crudo == null || crudo.isBlank()) {
            return Optional.empty();
        }

        String digitos = crudo.replaceAll("\\D", "");

        digitos = quitarPrefijoDePais(digitos);
        digitos = quitarCeroDeLargaDistancia(digitos);
        digitos = quitarQuinceDeCelular(digitos);

        if (digitos.length() != DIGITOS_NACIONALES) {
            return Optional.empty();
        }

        return Optional.of(PAIS + MOVIL + digitos);
    }

    /** Saca el 54 y el 9 de móvil si ya venían puestos. */
    private static String quitarPrefijoDePais(String digitos) {
        String resultado = digitos;

        if (resultado.startsWith(PAIS) && resultado.length() > DIGITOS_NACIONALES) {
            resultado = resultado.substring(PAIS.length());
        }
        if (resultado.startsWith(MOVIL) && resultado.length() == DIGITOS_NACIONALES + 1) {
            resultado = resultado.substring(MOVIL.length());
        }
        return resultado;
    }

    private static String quitarCeroDeLargaDistancia(String digitos) {
        return digitos.startsWith("0") ? digitos.substring(1) : digitos;
    }

    /**
     * Saca el 15 que se escribe entre el código de área y el abonado.
     *
     * Los códigos de área argentinos tienen entre 2 y 4 dígitos, así que no se
     * puede saber de antemano dónde cae el 15. Se prueba sacarlo en cada posición
     * posible y se acepta la que deje un número de largo válido. Si sacarlo no
     * mejora nada, se deja como estaba: puede ser un 15 que forma parte del
     * número real.
     */
    private static String quitarQuinceDeCelular(String digitos) {
        if (digitos.length() != DIGITOS_NACIONALES + 2) {
            return digitos;
        }

        for (int area = 2; area <= 4; area++) {
            if (digitos.startsWith("15", area)) {
                return digitos.substring(0, area) + digitos.substring(area + 2);
            }
        }
        return digitos;
    }

    /** Cómo mostrarlo en pantalla, para que el auditor confirme que es el correcto. */
    public static String paraMostrar(String normalizado) {
        if (normalizado == null || normalizado.length() != 13) {
            return normalizado;
        }
        return "+" + normalizado.substring(0, 2)
                + " " + normalizado.substring(2, 3)
                + " " + normalizado.substring(3, 7)
                + " " + normalizado.substring(7);
    }
}
