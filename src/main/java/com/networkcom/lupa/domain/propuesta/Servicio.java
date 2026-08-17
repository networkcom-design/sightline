package com.networkcom.lupa.domain.propuesta;

import com.networkcom.lupa.domain.auditoria.Senal;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Un servicio que vende Networkcom, con las señales que resuelve.
 *
 * Esta relación es la bisagra del producto: es lo que convierte un diagnóstico
 * en una propuesta comercial sin que nadie tenga que traducir a mano. Si la
 * auditoría detecta que la ficha de Google está sin horarios ni fotos, el
 * servicio que arregla eso aparece solo en el presupuesto.
 *
 * Los precios son valores de referencia y se editan desde la aplicación: cada
 * agencia cobra distinto y la inflación los mueve todo el tiempo.
 */
public record Servicio(
        String codigo,
        String nombre,
        String descripcion,
        Set<Senal> senalesQueResuelve,
        BigDecimal precioReferenciaArs,
        Modalidad modalidad,
        int plazoDias) {

    public enum Modalidad {
        /** Se cobra una vez: se hace, se entrega y se termina. */
        UNICO,
        /** Se cobra todos los meses porque requiere trabajo sostenido. */
        MENSUAL
    }

    /**
     * Cuántas de las señales que este servicio resuelve están hoy incumplidas.
     * Es lo que determina si vale la pena incluirlo en la propuesta.
     */
    public long hallazgosQueResuelve(Set<Senal> senalesIncumplidas) {
        return senalesQueResuelve.stream().filter(senalesIncumplidas::contains).count();
    }

    public boolean esRecurrente() {
        return modalidad == Modalidad.MENSUAL;
    }
}
