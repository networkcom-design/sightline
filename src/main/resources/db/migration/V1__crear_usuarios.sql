-- Tabla de usuarios: cada DT que use PizarrIA tiene su cuenta.
CREATE TABLE usuarios (
    id              UUID         PRIMARY KEY,
    email           VARCHAR(180) NOT NULL,
    contrasena_hash VARCHAR(72)  NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- El email identifica la cuenta y se compara en minusculas, asi que el indice
-- unico va sobre la version normalizada para que "DT@Club.com" y "dt@club.com"
-- no puedan registrarse dos veces.
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (LOWER(email));
