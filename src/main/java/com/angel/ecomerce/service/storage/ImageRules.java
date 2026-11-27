package com.angel.ecomerce.service.storage;

import com.angel.ecomerce.config.StorageProperties;
import com.angel.ecomerce.dto.storage.ImageUpload;

import java.util.UUID;

/**
 * Reglas de imagen comunes a productos y categorías.
 *
 * <p>Validan tipo y tamaño antes de tocar el bucket y construyen la clave del
 * objeto. Son reglas de dominio: fallan igual con B2 o con cualquier otro
 * almacenamiento.</p>
 */
public final class ImageRules {

    /** Claves del bucket: {@code <prefijo>/<id>/<uuid>.<ext>}. */
    private static final String IMAGE_KEY_PATTERN = "%s/%d/%s.%s";

    private ImageRules() {
    }

    public static void requireValid(ImageUpload image, StorageProperties properties) {
        if (image == null || !image.hasContent()) {
            throw new IllegalArgumentException("La imagen está vacía");
        }
        String contentType = image.contentTypeOrDefault();
        if (!properties.getAllowedContentTypes().contains(contentType)) {
            throw new IllegalArgumentException("Tipo de imagen no permitido (" + contentType
                    + "). Permitidos: " + properties.getAllowedContentTypes());
        }
        if (image.size() > properties.getMaxFileSize()) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo de "
                    + properties.getMaxFileSize() + " bytes");
        }
    }

    public static String keyFor(String prefix, Long id, ImageUpload image) {
        return IMAGE_KEY_PATTERN.formatted(prefix, id, UUID.randomUUID(), image.extension());
    }
}
