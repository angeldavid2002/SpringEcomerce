-- Clave del objeto de imagen en el bucket (Backblaze B2).
-- image_url se mantiene para URLs externas; si un producto tiene image_key,
-- la imagen del bucket manda y image_url se ignora.
ALTER TABLE products ADD COLUMN image_key VARCHAR(512) NULL;