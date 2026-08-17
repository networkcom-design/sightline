package com.networkcom.lupa.domain.propuesta;

import java.math.BigDecimal;

/**
 * Lo que el auditor cambió de un servicio en una propuesta concreta.
 *
 * Es por auditoría y no global: bajarle el precio al sitio para un comercio
 * chico no tiene que tocar el catálogo ni las propuestas ya enviadas. El
 * catálogo define lo que Networkcom vende; esto, lo que se le cotizó a este
 * cliente.
 *
 * `precio` en nulo significa "usar el de referencia". Guardar el valor del
 * catálogo copiado haría que un cambio de lista no se refleje en las propuestas
 * que todavía no se tocaron.
 */
public record AjusteServicio(boolean incluido, BigDecimal precio) {

    /** El estado por defecto: entra al presupuesto, al precio de lista. */
    public static AjusteServicio porDefecto() {
        return new AjusteServicio(true, null);
    }

    public static AjusteServicio excluido() {
        return new AjusteServicio(false, null);
    }

    public AjusteServicio conPrecio(BigDecimal nuevo) {
        return new AjusteServicio(incluido, nuevo);
    }

    public AjusteServicio incluir(boolean valor) {
        return new AjusteServicio(valor, precio);
    }

    /** El precio que corresponde cobrar: el ajustado si hay, si no el de lista. */
    public BigDecimal precioEfectivo(Servicio servicio) {
        return precio != null ? precio : servicio.precioReferenciaArs();
    }
}
