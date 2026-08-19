package com.networkcom.lupa.domain.propuesta;

import com.networkcom.lupa.domain.auditoria.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un servicio que el comercio ya contrató, con su estado de ejecución.
 *
 * Es una copia y no una referencia al catálogo, y eso es deliberado. El nombre,
 * el precio, la modalidad y el plazo se congelan el día que el cliente acepta.
 * Si mañana sube la lista de precios o se renombra un servicio, lo que se
 * pactó con este comercio sigue diciendo lo mismo: un presupuesto aceptado es
 * un compromiso, no una vista de la tabla de precios de hoy.
 */
@Entity
@Table(name = "tareas_contratadas")
public class TareaContratada {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private Auditoria auditoria;

    @Column(name = "codigo_servicio", nullable = false, length = 40)
    private String codigoServicio;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Servicio.Modalidad modalidad;

    @Column(name = "plazo_dias", nullable = false)
    private int plazoDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTarea estado;

    @Column(name = "iniciada_en")
    private Instant iniciadaEn;

    @Column(name = "completada_en")
    private Instant completadaEn;

    @Column(length = 400)
    private String nota;

    @Column(nullable = false)
    private int orden;

    protected TareaContratada() {
    }

    public TareaContratada(Auditoria auditoria, Servicio servicio, BigDecimal precioAcordado, int orden) {
        this.id = UUID.randomUUID();
        this.auditoria = auditoria;
        this.codigoServicio = servicio.codigo();
        this.nombre = servicio.nombre();
        this.precio = precioAcordado;
        this.modalidad = servicio.modalidad();
        this.plazoDias = servicio.plazoDias();
        this.estado = EstadoTarea.PENDIENTE;
        this.orden = orden;
    }

    /**
     * Mueve la tarea de estado y registra cuándo.
     *
     * Las fechas se escriben una sola vez, la primera. Si una tarea vuelve
     * atrás porque el cliente pidió un cambio y después se vuelve a completar,
     * la fecha de inicio sigue siendo la real: es la que sirve para saber
     * cuánto tardó de verdad, no cuántas veces se tocó el botón.
     */
    public void cambiarEstado(EstadoTarea nuevo, String nota) {
        this.estado = nuevo;
        this.nota = nota == null || nota.isBlank() ? null : nota.trim();

        if (nuevo != EstadoTarea.PENDIENTE && iniciadaEn == null) {
            this.iniciadaEn = Instant.now();
        }
        if (nuevo == EstadoTarea.COMPLETADA && completadaEn == null) {
            this.completadaEn = Instant.now();
        }
    }

    /**
     * Si el comercio ya está recibiendo lo que contrató.
     *
     * Un abono mensual en curso ya está entregando valor todos los meses; no
     * hay un día en que se termine. Exigirle el estado COMPLETADA para contarlo
     * como cumplido dejaría el avance clavado por debajo del 100% mientras el
     * trabajo corre normalmente, que es exactamente al revés de la realidad.
     */
    public boolean estaCumplida() {
        return esRecurrente()
                ? estado == EstadoTarea.EN_CURSO || estado == EstadoTarea.COMPLETADA
                : estado == EstadoTarea.COMPLETADA;
    }

    public boolean esRecurrente() {
        return modalidad == Servicio.Modalidad.MENSUAL;
    }

    /** Cómo se lee el estado actual según la modalidad. */
    public String etiquetaDeEstado() {
        return etiquetaDe(estado, modalidad);
    }

    /**
     * El nombre que le pone la agencia a cada estado.
     *
     * Vive acá y no en el frontend porque lo necesitan tres pantallas —el
     * seguimiento, el informe impreso y lo que ve el cliente— y tener la
     * traducción repetida en dos lenguajes garantiza que en algún momento digan
     * cosas distintas para el mismo estado.
     */
    public static String etiquetaDe(EstadoTarea estado, Servicio.Modalidad modalidad) {
        if (modalidad == Servicio.Modalidad.MENSUAL) {
            return switch (estado) {
                case PENDIENTE -> "Sin arrancar";
                case EN_CURSO -> "Activo";
                case COMPLETADA -> "Dado de baja";
            };
        }
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_CURSO -> "En curso";
            case COMPLETADA -> "Entregado";
        };
    }

    public UUID getId() {
        return id;
    }

    public String getCodigoServicio() {
        return codigoServicio;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public Servicio.Modalidad getModalidad() {
        return modalidad;
    }

    public int getPlazoDias() {
        return plazoDias;
    }

    public EstadoTarea getEstado() {
        return estado;
    }

    public Instant getIniciadaEn() {
        return iniciadaEn;
    }

    public Instant getCompletadaEn() {
        return completadaEn;
    }

    public String getNota() {
        return nota;
    }

    public int getOrden() {
        return orden;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof TareaContratada tarea)) {
            return false;
        }
        return Objects.equals(id, tarea.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
