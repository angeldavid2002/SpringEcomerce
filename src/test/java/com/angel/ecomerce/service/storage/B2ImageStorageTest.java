package com.angel.ecomerce.service.storage;

import com.angel.ecomerce.config.StorageProperties;
import com.angel.ecomerce.dto.storage.ImageUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba la URL firmada que se devuelve al frontend.
 *
 * <p>No necesita bucket ni red: el presigner solo construye y firma la URL en
 * local. Estas aserciones cubren dos bugs reales: devolver el {@code toString()}
 * del objeto presignado (texto basura) y generarla en estilo virtual-host, que
 * Backblaze B2 no soporta.</p>
 */
class B2ImageStorageTest {

    private B2ImageStorage storage;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setEnabled(true);
        properties.setEndpoint("https://s3.us-west-004.backblazeb2.com");
        properties.setRegion("us-west-004");
        properties.setBucket("mi-bucket");
        properties.setAccessKey("004clavedummy");
        properties.setSecretKey("secreto-dummy-de-al-menos-32-bytes!!");
        properties.setPresignExpiration(Duration.ofMinutes(30));
        storage = new B2ImageStorage(properties);
    }

    @Test
    void presignedUrlUsesPathStyleAndTheConfiguredEndpoint() {
        String url = storage.presignedUrl("products/10/clave.png");

        assertEquals("https://s3.us-west-004.backblazeb2.com/mi-bucket/products/10/clave.png",
                url.substring(0, url.indexOf('?')));
        assertFalse(url.contains("mi-bucket.s3."),
                "B2 no admite estilo virtual-host: el bucket debe ir en la ruta");
    }

    @Test
    void presignedUrlCarriesTheSignatureAndTheConfiguredExpiration() {
        String url = storage.presignedUrl("products/10/clave.png");

        assertTrue(url.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"), url);
        assertTrue(url.contains("X-Amz-Signature="), url);
        assertTrue(url.contains("X-Amz-Expires=1800"), "la caducidad debe salir de B2_PRESIGN_EXPIRATION");
        assertTrue(url.contains("X-Amz-Credential=004clavedummy%2F"), url);
    }

    @Test
    void presignedUrlIsNullForBlankKeys() {
        assertNull(storage.presignedUrl(null));
        assertNull(storage.presignedUrl("   "));
    }

    @Test
    void storageIsEnabled() {
        assertTrue(storage.isEnabled());
    }

    @Test
    void storeRejectsAnEmptyImage() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.store("products/1/x.png", new ImageUpload(new byte[0], "image/png", "x.png")));
        assertThrows(IllegalArgumentException.class,
                () -> storage.store("products/1/x.png", null));
    }

    @Test
    void deleteIgnoresBlankKeys() {
        storage.delete(null);
        storage.delete("  ");
    }
}