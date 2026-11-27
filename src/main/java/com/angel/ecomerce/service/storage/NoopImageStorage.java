package com.angel.ecomerce.service.storage;

import com.angel.ecomerce.dto.storage.ImageUpload;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Almacenamiento desactivado.
 *
 * <p>Permite arrancar y probar la API sin bucket: los productos se crean igual
 * y las subidas de imagen responden {@code 503} con un mensaje claro en lugar de
 * un error opaco.</p>
 */
@Service
@ConditionalOnProperty(prefix = "app.storage.b2", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopImageStorage implements IImageStorage {

    @Override
    public String store(String key, ImageUpload upload) {
        throw new ImageStorageNotConfiguredException();
    }

    @Override
    public void delete(String key) {
        // Nada que borrar.
    }

    @Override
    public String presignedUrl(String key) {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}