USE basededatos;

DROP TABLE IF EXISTS empresa;
DROP TABLE IF EXISTS persona;
DROP TABLE IF EXISTS pedido;
DROP TABLE IF EXISTS cliente;

CREATE TABLE cliente (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    tipo ENUM('PERSONA', 'EMPRESA') NOT NULL,
    fecha_alta DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE persona (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    dni VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(25),
    CONSTRAINT fk_persona_cliente
        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE empresa (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL UNIQUE,
    razon_social VARCHAR(150) NOT NULL,
    cif VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(25),
    sector VARCHAR(80),
    CONSTRAINT fk_empresa_cliente
        FOREIGN KEY (cliente_id) REFERENCES cliente(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE pedido (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fecha DATE NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    cliente_id BIGINT NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_pedido_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB;
