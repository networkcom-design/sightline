package com.networkcom.lupa.application.ia;

/**
 * Puerto hacia el modelo de lenguaje.
 *
 * Es una interfaz mínima a propósito: recibe instrucciones y un texto, devuelve
 * la respuesta cruda. Toda la inteligencia de qué preguntar y cómo interpretar
 * la respuesta vive en la capa de aplicación, no acá.
 *
 * Tenerlo así permite dos cosas: cambiar de proveedor sin tocar la lógica, y
 * probar el análisis con un proveedor falso sin gastar la cuota real.
 */
public interface ProveedorIA {

    /**
     * Pide texto común.
     *
     * @param instrucciones qué rol cumple y qué formato tiene que devolver
     * @param contenido     la evidencia sobre la que trabajar
     * @return la respuesta del modelo, sin procesar
     */
    String responder(String instrucciones, String contenido);

    /**
     * Pide una respuesta en JSON, avisándole al modelo por el canal que
     * corresponda en lugar de rogárselo en el prompt.
     *
     * Están separados porque configurar salida JSON para todo tiene un efecto
     * que no se ve hasta que aparece: cuando después le pedís prosa, te devuelve
     * la prosa envuelta en una cadena JSON, con comillas y con los saltos de
     * línea escapados.
     */
    default String responderJson(String instrucciones, String contenido) {
        return responder(instrucciones, contenido);
    }

    /** Nombre del proveedor, para mostrarlo en el informe y en los registros. */
    String nombre();

    class ProveedorIAException extends RuntimeException {
        public ProveedorIAException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }

        public ProveedorIAException(String mensaje) {
            super(mensaje);
        }
    }
}
