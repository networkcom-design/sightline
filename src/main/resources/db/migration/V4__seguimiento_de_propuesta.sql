-- Que pasa despues de mandar la propuesta.
--
-- Hasta aca el sistema terminaba en "enviada" y lo que seguia vivia en la
-- cabeza del vendedor: si el comercio contesto, si acepto, que se le prometio
-- y cuanto de eso esta hecho. Estas dos cosas cierran ese hueco.

-- 1. Las fechas del expediente. Van en la auditoria y no en una tabla de
--    historial porque cada una ocurre una sola vez y siempre se leen junto con
--    el resto: un historial completo de transiciones seria mas caro de mantener
--    y no responde ninguna pregunta que hoy alguien se haga.
ALTER TABLE auditorias ADD COLUMN aceptada_en     TIMESTAMPTZ;
ALTER TABLE auditorias ADD COLUMN rechazada_en    TIMESTAMPTZ;
ALTER TABLE auditorias ADD COLUMN motivo_rechazo  VARCHAR(400);
ALTER TABLE auditorias ADD COLUMN entregada_en    TIMESTAMPTZ;

-- 2. Lo contratado, congelado el dia que el cliente acepta.
--
-- El nombre, el precio, la modalidad y el plazo se copian en lugar de
-- referenciar el catalogo. Un presupuesto aceptado es un compromiso: si mañana
-- sube la lista o se renombra un servicio, lo que se pacto con este comercio
-- tiene que seguir diciendo lo mismo.
--
-- Esta si es una tabla y no JSON, al reves que ajustes_propuesta: cada fila
-- cambia por su cuenta a lo largo de semanas, tiene fechas propias y se
-- consulta por estado para saber que hay pendiente en toda la cartera.
CREATE TABLE tareas_contratadas (
    id               UUID           PRIMARY KEY,
    auditoria_id     UUID           NOT NULL REFERENCES auditorias (id) ON DELETE CASCADE,
    codigo_servicio  VARCHAR(40)    NOT NULL,
    nombre           VARCHAR(160)   NOT NULL,
    precio           NUMERIC(12, 2) NOT NULL,
    modalidad        VARCHAR(20)    NOT NULL,
    plazo_dias       INTEGER        NOT NULL,
    estado           VARCHAR(20)    NOT NULL,
    iniciada_en      TIMESTAMPTZ,
    completada_en    TIMESTAMPTZ,
    nota             VARCHAR(400),
    orden            INTEGER        NOT NULL
);

-- Un servicio no se puede contratar dos veces en la misma auditoria: la tarea
-- se identifica por su codigo al actualizarla, y dos filas con el mismo codigo
-- harian que el avance se registre en cualquiera de las dos.
CREATE UNIQUE INDEX ux_tareas_auditoria_servicio
    ON tareas_contratadas (auditoria_id, codigo_servicio);

CREATE INDEX ix_tareas_auditoria ON tareas_contratadas (auditoria_id, orden);
