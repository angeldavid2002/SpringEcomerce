-- Clave del objeto de imagen de la categoría en el bucket (Backblaze B2).
-- La URL que ve el frontend se firma al vuelo a partir de esta clave.
ALTER TABLE categories ADD COLUMN image_key VARCHAR(512) NULL;