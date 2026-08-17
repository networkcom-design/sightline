CREATE TABLE auditorias (
    id         UUID         PRIMARY KEY,
    usuario_id UUID         NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,

    nombre     VARCHAR(160) NOT NULL,
    rubro      VARCHAR(120) NOT NULL,
    ciudad     VARCHAR(120),
    telefono   VARCHAR(60),
    direccion  VARCHAR(200),
    sitio_web  VARCHAR(300),

    -- La evidencia se guarda tal cual la pegó el auditor. Sirve para reproducir
    -- un dictamen que se discuta y para reanalizar si mejora el prompt, sin
    -- tener que volver a abrir los perfiles.
    texto_instagram TEXT,
    texto_google    TEXT,
    notas           TEXT,

    -- La medicion del sitio va como JSON en una sola columna: es una foto de un
    -- momento, se lee entera o no se lee, y nunca se consulta por sus campos.
    medicion   TEXT,

    estado     VARCHAR(30)  NOT NULL,

    -- Token del enlace publico. Es la unica credencial para ver el informe, asi
    -- que tiene que ser largo y aleatorio: quien lo tenga, entra.
    token_publico VARCHAR(64) NOT NULL,

    creada_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_auditorias_token ON auditorias (token_publico);
CREATE INDEX ix_auditorias_usuario ON auditorias (usuario_id, creada_en DESC);

CREATE TABLE respuestas_senal (
    id           UUID        PRIMARY KEY,
    auditoria_id UUID        NOT NULL REFERENCES auditorias (id) ON DELETE CASCADE,

    senal        VARCHAR(40) NOT NULL,
    estado       VARCHAR(20) NOT NULL,
    origen       VARCHAR(20) NOT NULL,
    confianza    VARCHAR(10) NOT NULL,
    fundamento   VARCHAR(300)
);

-- Una senal se responde una sola vez por auditoria. Si la IA dictamina y
-- despues el auditor corrige, se actualiza la misma fila y queda la ultima
-- palabra, que es siempre la humana.
CREATE UNIQUE INDEX ux_respuestas_senal ON respuestas_senal (auditoria_id, senal);
