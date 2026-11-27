package com.angel.ecomerce.service.storage;

import com.angel.ecomerce.dto.storage.ImageUpload;

/**
 * Almacenamiento de imágenes en un bucket S3 (Backblaze B2).
 *
 * <p>La aplicación solo guarda la <em>clave</em> del objeto y devuelve al
 * cliente URLs firmadas de un solo uso: el bucket es privado y el frontend
 * nunca ve una ruta pública.</p>
 */
public interface IImageStorage {

    /**
     * Sube la imagen y devuelve la clave con la que quedó guardada.
     * Valida tipo y tamaño antes de escribir.
     */
    String store(String key, ImageUpload upload);

    /** Borra el objeto del bucket. Si no existe, no hace nada. */
    void delete(String key);

    /** URL firmada y temporal para leer el objeto; {@code null} si la clave está vacía. */
    String presignedUrl(String key);

    /** true si hay un bucket configurado. */
    boolean isEnabled();
}