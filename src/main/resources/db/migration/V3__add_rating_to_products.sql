-- Añadir columna de calificación (rating) a productos para filtrado por puntuación
ALTER TABLE products ADD COLUMN rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00;
