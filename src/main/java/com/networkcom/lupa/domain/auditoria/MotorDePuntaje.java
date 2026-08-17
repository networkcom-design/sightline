package com.networkcom.lupa.domain.auditoria;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte las respuestas en puntajes, hallazgos y plan de acción.
 *
 * Es lógica pura y determinística: no toca la base, no llama a la IA y no
 * depende de la hora. Con las mismas respuestas devuelve siempre el mismo
 * informe, y eso es lo que hace que el puntaje sea defendible cuando el dueño
 * del comercio pregunta de dónde salió el número.
 *
 * La IA entra recién después, para redactar el resumen y contextualizar los
 * hallazgos. Nunca para calcular.
 */
public final class MotorDePuntaje {

    private MotorDePuntaje() {
    }

    /** Puntaje de una dimensión, con el detalle de cómo se compuso. */
    public record PuntajeDimension(
            Dimension dimension,
            int puntaje,
            int senalesCumplidas,
            int senalesEvaluadas,
            List<Hallazgo> hallazgos) {
    }

    /** Algo que está mal y qué hacer al respecto. */
    public record Hallazgo(
            String codigo,
            Dimension dimension,
            String titulo,
            String accion,
            Impacto impacto,
            Esfuerzo esfuerzo,
            int minutosEstimados) {

        static Hallazgo de(Senal senal) {
            return new Hallazgo(
                    senal.name(),
                    senal.getDimension(),
                    senal.getHallazgo(),
                    senal.getAccion(),
                    senal.getImpacto(),
                    senal.getEsfuerzo(),
                    senal.getMinutosEstimados());
        }
    }

    public record Resultado(
            int puntajeGlobal,
            NivelPresencia nivel,
            List<PuntajeDimension> dimensiones,
            List<Hallazgo> planDeAccion,
            int senalesEvaluadas,
            int senalesCumplidas,
            int minutosTotalesEstimados) {
    }

    /**
     * Calcula el informe completo.
     *
     * Dentro de cada dimensión el puntaje se normaliza por el peso de las
     * señales que sí aplican, no por el peso total. Así una dimensión donde la
     * mitad de las señales no corresponden sigue puntuando de 0 a 100.
     */
    public static Resultado calcular(Map<Senal, EstadoSenal> respuestas) {
        Map<Dimension, PuntajeDimension> porDimension = new EnumMap<>(Dimension.class);

        int evaluadasTotales = 0;
        int cumplidasTotales = 0;

        for (Dimension dimension : Dimension.values()) {
            int pesoAplicable = 0;
            int pesoCumplido = 0;
            int cumplidas = 0;
            int evaluadas = 0;

            List<Hallazgo> hallazgos = new java.util.ArrayList<>();

            for (Senal senal : Senal.de(dimension)) {
                EstadoSenal estado = respuestas.getOrDefault(senal, EstadoSenal.NO_APLICA);

                if (estado == EstadoSenal.NO_APLICA) {
                    continue;
                }

                pesoAplicable += senal.getPeso();
                evaluadas++;

                if (estado == EstadoSenal.CUMPLE) {
                    pesoCumplido += senal.getPeso();
                    cumplidas++;
                } else {
                    hallazgos.add(Hallazgo.de(senal));
                }
            }

            // Una dimensión sin ninguna señal aplicable puntúa 100: no se le
            // puede reclamar nada, así que no debe arrastrar el global hacia abajo.
            int puntaje = pesoAplicable == 0
                    ? 100
                    : (int) Math.round(100.0 * pesoCumplido / pesoAplicable);

            hallazgos.sort(comparadorDePrioridad());
            porDimension.put(dimension, new PuntajeDimension(dimension, puntaje, cumplidas, evaluadas, hallazgos));

            evaluadasTotales += evaluadas;
            cumplidasTotales += cumplidas;
        }

        int puntajeGlobal = calcularGlobal(porDimension);

        List<Hallazgo> plan = porDimension.values().stream()
                .flatMap(pd -> pd.hallazgos().stream())
                .sorted(comparadorDePrioridad())
                .toList();

        int minutos = plan.stream().mapToInt(Hallazgo::minutosEstimados).sum();

        return new Resultado(
                puntajeGlobal,
                NivelPresencia.desdePuntaje(puntajeGlobal),
                List.copyOf(porDimension.values()),
                plan,
                evaluadasTotales,
                cumplidasTotales,
                minutos);
    }

    private static int calcularGlobal(Map<Dimension, PuntajeDimension> porDimension) {
        double acumulado = 0;

        for (Dimension dimension : Dimension.values()) {
            PuntajeDimension puntaje = porDimension.get(dimension);
            acumulado += puntaje.puntaje() * dimension.getPeso();
        }

        // Los pesos de las dimensiones suman 100, condición que se verifica al
        // arrancar la aplicación, así que dividir por 100 devuelve la escala.
        return (int) Math.round(acumulado / 100.0);
    }

    /**
     * Primero lo que más mueve la aguja con menos trabajo; ante igual relación,
     * primero lo de mayor impacto; y si siguen empatados, lo más rápido.
     */
    private static Comparator<Hallazgo> comparadorDePrioridad() {
        return Comparator
                .comparingDouble((Hallazgo h) -> (double) h.impacto().getValor() / h.esfuerzo().getValor())
                .reversed()
                .thenComparing(Comparator.comparingInt((Hallazgo h) -> h.impacto().getValor()).reversed())
                .thenComparingInt(Hallazgo::minutosEstimados);
    }
}
