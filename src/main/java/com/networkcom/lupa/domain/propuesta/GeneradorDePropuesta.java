package com.networkcom.lupa.domain.propuesta;

import com.networkcom.lupa.domain.auditoria.EstadoSenal;
import com.networkcom.lupa.domain.auditoria.MotorDePuntaje;
import com.networkcom.lupa.domain.auditoria.Senal;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte los hallazgos de una auditoría en un presupuesto.
 *
 * El número que hace la diferencia en una reunión de venta es el puntaje
 * proyectado: no alcanza con decirle al comercio que está en 42, hay que poder
 * mostrarle a cuánto llega si contrata. Y como el proyectado se calcula con el
 * mismo motor que el actual —marcando como cumplidas las señales que resuelven
 * los servicios elegidos— no es una promesa inventada, es la misma cuenta.
 */
public final class GeneradorDePropuesta {

    private GeneradorDePropuesta() {
    }

    /**
     * Una línea del presupuesto.
     *
     * Los servicios excluidos siguen apareciendo en la lista, marcados como no
     * incluidos: el auditor tiene que poder volver a sumarlos, y ver qué está
     * dejando afuera es parte de la conversación con el cliente.
     */
    public record Item(
            Servicio servicio,
            long hallazgosQueResuelve,
            BigDecimal precio,
            boolean incluido) {
    }

    /**
     * El presupuesto, con dos proyecciones en lugar de una.
     *
     * `puntajeAlEntregar` cuenta solo los servicios de pago único: es lo que el
     * comercio va a ver el día que se termina el trabajo, y por eso es el número
     * creíble para prometer. `puntajeSostenido` suma lo que aportan los
     * servicios mensuales, que resuelven señales que dependen de sostener el
     * trabajo —publicar seguido, juntar reseñas, responder mensajes— y que
     * vuelven a caerse si se corta el abono.
     *
     * Separarlos no es un tecnicismo: es el argumento de por qué el abono
     * mensual no es opcional.
     */
    public record Propuesta(
            List<Item> items,
            BigDecimal totalUnico,
            BigDecimal totalMensual,
            int plazoDiasEstimado,
            int puntajeActual,
            int puntajeAlEntregar,
            int puntajeSostenido,
            List<Senal> hallazgosSinCubrir) {

        public int mejoraAlEntregar() {
            return puntajeAlEntregar - puntajeActual;
        }

        public int loQueAportaElAbono() {
            return puntajeSostenido - puntajeAlEntregar;
        }

        public boolean estaVacia() {
            return items.isEmpty();
        }
    }

    /** Arma el borrador con todos los servicios que resuelven algún hallazgo. */
    public static Propuesta generar(Map<Senal, EstadoSenal> respuestas, List<Servicio> catalogo) {
        return generar(respuestas, catalogo, Map.of());
    }

    /**
     * Arma la propuesta aplicando los ajustes del auditor.
     *
     * Un servicio sin ajuste entra al precio de lista: el estado por defecto es
     * "todo lo que hace falta, a lo que sale". Los ajustes solo registran las
     * decisiones que se apartan de eso.
     */
    public static Propuesta generar(Map<Senal, EstadoSenal> respuestas,
                                    List<Servicio> catalogo,
                                    Map<String, AjusteServicio> ajustes) {
        Set<Senal> incumplidas = incumplidas(respuestas);

        List<Servicio> aplicables = catalogo.stream()
                .filter(servicio -> servicio.hallazgosQueResuelve(incumplidas) > 0)
                .toList();

        return armar(respuestas, aplicables, incumplidas, ajustes);
    }

    /**
     * Recalcula la propuesta con un subconjunto de servicios.
     *
     * Es lo que permite que, al sacar un servicio del presupuesto porque el
     * cliente no lo quiere, el puntaje proyectado baje en el acto y se vea qué
     * se está resignando.
     */
    public static Propuesta conServicios(Map<Senal, EstadoSenal> respuestas, List<Servicio> elegidos) {
        return armar(respuestas, elegidos, incumplidas(respuestas), Map.of());
    }

    private static Propuesta armar(Map<Senal, EstadoSenal> respuestas,
                                   List<Servicio> servicios,
                                   Set<Senal> incumplidas,
                                   Map<String, AjusteServicio> ajustes) {

        List<Item> items = servicios.stream()
                .map(servicio -> {
                    AjusteServicio ajuste = ajustes.getOrDefault(servicio.codigo(), AjusteServicio.porDefecto());
                    return new Item(
                            servicio,
                            servicio.hallazgosQueResuelve(incumplidas),
                            ajuste.precioEfectivo(servicio),
                            ajuste.incluido());
                })
                // Primero lo que resuelve más problemas: es el orden en que
                // conviene presentarlo y también el orden en que conviene hacerlo.
                .sorted(Comparator.comparingLong(Item::hallazgosQueResuelve).reversed()
                        .thenComparing(item -> item.servicio().nombre()))
                .toList();

        List<Servicio> incluidos = items.stream()
                .filter(Item::incluido)
                .map(Item::servicio)
                .toList();

        BigDecimal totalUnico = sumar(items, false);
        BigDecimal totalMensual = sumar(items, true);

        // El plazo cuenta solo los servicios de pago único: son los que se
        // entregan y terminan. Un abono mensual no tiene fecha de entrega, y
        // dejar que su ciclo de 30 días se cuele acá haría que una propuesta que
        // se termina en tres semanas se prometa en un mes.
        // Entre los únicos, el plazo es el del más largo y no la suma: se
        // ejecutan en paralelo.
        int plazo = items.stream()
                .filter(item -> item.incluido() && !item.servicio().esRecurrente())
                .mapToInt(item -> item.servicio().plazoDias())
                .max()
                .orElse(0);

        List<Servicio> unicos = incluidos.stream().filter(servicio -> !servicio.esRecurrente()).toList();

        int puntajeActual = MotorDePuntaje.calcular(respuestas).puntajeGlobal();
        int puntajeAlEntregar = MotorDePuntaje.calcular(proyectar(respuestas, unicos)).puntajeGlobal();
        int puntajeSostenido = MotorDePuntaje.calcular(proyectar(respuestas, incluidos)).puntajeGlobal();

        // Lo que queda sin cubrir se mide contra lo que efectivamente entra al
        // presupuesto: si el cliente saca un servicio, sus hallazgos vuelven a
        // quedar pendientes y hay que poder decirlo.
        Set<Senal> cubiertas = incluidos.stream()
                .flatMap(servicio -> servicio.senalesQueResuelve().stream())
                .collect(Collectors.toSet());

        List<Senal> sinCubrir = incumplidas.stream()
                .filter(senal -> !cubiertas.contains(senal))
                .sorted(Comparator.comparing(Senal::name))
                .toList();

        return new Propuesta(items, totalUnico, totalMensual, plazo,
                puntajeActual, puntajeAlEntregar, puntajeSostenido, sinCubrir);
    }

    /**
     * Simula cómo quedaría la auditoría después de ejecutar los servicios.
     * Solo toca las señales incumplidas: lo que ya estaba bien sigue igual, y
     * lo que no aplica sigue sin aplicar.
     */
    private static Map<Senal, EstadoSenal> proyectar(Map<Senal, EstadoSenal> respuestas,
                                                     List<Servicio> servicios) {
        // Se construye vacío y se copia: el constructor de copia de EnumMap
        // falla si le pasan un mapa vacío que no sea a su vez un EnumMap.
        Map<Senal, EstadoSenal> proyeccion = new EnumMap<>(Senal.class);
        proyeccion.putAll(respuestas);

        Set<Senal> aResolver = servicios.stream()
                .flatMap(servicio -> servicio.senalesQueResuelve().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Senal senal : aResolver) {
            if (proyeccion.get(senal) == EstadoSenal.NO_CUMPLE) {
                proyeccion.put(senal, EstadoSenal.CUMPLE);
            }
        }

        return proyeccion;
    }

    private static Set<Senal> incumplidas(Map<Senal, EstadoSenal> respuestas) {
        return respuestas.entrySet().stream()
                .filter(entrada -> entrada.getValue() == EstadoSenal.NO_CUMPLE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Solo suma lo que está incluido: lo excluido se muestra pero no se cobra. */
    private static BigDecimal sumar(List<Item> items, boolean recurrentes) {
        return items.stream()
                .filter(Item::incluido)
                .filter(item -> item.servicio().esRecurrente() == recurrentes)
                .map(Item::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
