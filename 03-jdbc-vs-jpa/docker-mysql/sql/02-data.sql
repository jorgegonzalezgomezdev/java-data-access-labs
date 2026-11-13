USE basededatos;

INSERT INTO cliente (id, codigo, tipo, fecha_alta, activo) VALUES
    (1, 'CLI-001', 'PERSONA', '2026-01-15', TRUE),
    (2, 'CLI-002', 'PERSONA', '2026-02-03', TRUE),
    (3, 'CLI-003', 'PERSONA', '2026-02-20', FALSE),
    (4, 'CLI-004', 'EMPRESA', '2026-03-01', TRUE),
    (5, 'CLI-005', 'EMPRESA', '2026-03-12', TRUE);

INSERT INTO persona
    (id, cliente_id, nombre, apellidos, dni, email, telefono)
VALUES
    (1, 1, 'Ana', 'García López', '12345678A',
        'ana.garcia@example.com', '600111222'),
    (2, 2, 'Luis', 'Martín Sanz', '23456789B',
        'luis.martin@example.com', '600333444'),
    (3, 3, 'Marta', 'Sánchez Ruiz', '34567890C',
        'marta.sanchez@example.com', '600555666');

INSERT INTO empresa
    (id, cliente_id, razon_social, cif, email, telefono, sector)
VALUES
    (1, 4, 'TecnoSoluciones S.L.', 'B12345678',
        'contacto@tecnosoluciones.example', '910111222', 'Tecnología'),
    (2, 5, 'VerdeNorte S.A.', 'A87654321',
        'info@verdenorte.example', '910333444', 'Energía');

INSERT INTO pedido (fecha, total, cliente_id) VALUES
('2026-10-01', 125.50, 1),
('2026-10-03', 89.99, 2),
('2026-10-05', 240.00, 1),
('2026-10-08', 45.75, 3),
('2026-10-12', 310.20, 2);

ALTER TABLE cliente AUTO_INCREMENT = 6;
ALTER TABLE persona AUTO_INCREMENT = 4;
ALTER TABLE empresa AUTO_INCREMENT = 3;
