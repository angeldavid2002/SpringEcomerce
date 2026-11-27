package com.angel.ecomerce.service.storage;

/**
 * Se lanza cuando se pide subir o firmar una imagen sin bucket configurado.
 * El handler la traduce a {@code 503 Service Unavailable}.
 */
public class ImageStorageNotConfiguredException extends RuntimeException {

    public ImageStorageNotConfiguredException() {
        super("El almacenamiento de imágenes no está configurado. Revisa B2_ENABLED y las variables B2_* en .env");
    }
}