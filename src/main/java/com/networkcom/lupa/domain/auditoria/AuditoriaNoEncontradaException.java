package com.networkcom.lupa.domain.auditoria;

/**
 * No existe la auditoría, o existe pero es de otro usuario.
 *
 * Los dos casos comparten excepción a propósito: distinguirlos le confirmaría a
 * un curioso que ese identificador existe aunque no pueda verlo.
 */
public class AuditoriaNoEncontradaException extends RuntimeException {

    public AuditoriaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
