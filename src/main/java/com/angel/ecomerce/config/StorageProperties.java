package com.angel.ecomerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuración del almacenamiento de imágenes en Backblaze B2 (API S3).
 *
 * <p>Todo se lee del entorno o de {@code .env}; no hay credenciales en el
 * repositorio. Mientras {@code app.storage.b2.enabled} sea {@code false} la
 * aplicación arranca igual y solo rechaza (503) las subidas de imagen, para
 * poder levantar el proyecto sin bucket.</p>
 */
@ConfigurationProperties(prefix = "app.storage.b2")
@Data
public class StorageProperties {

    /** Activa el cliente real contra B2. En false se usa {@code NoopImageStorage}. */
    private boolean enabled = false;

    /** Endpoint del bucket, con esquema. Ejemplo: {@code https://s3.us-west-004.backblazeb2.com}. */
    private String endpoint;

    /** Región del bucket, la misma del endpoint. Ejemplo: {@code us-west-004}. */
    private String region;

    /** Nombre del bucket (privado). */
    private String bucket;

    /** {@code keyID} de la application key de B2. */
    private String accessKey;

    /** {@code applicationKey} de B2. */
    private String secretKey;

    /** Vida de la URL firmada que se devuelve al frontend. */
    private Duration presignExpiration = Duration.ofMinutes(30);

    /** Tamaño máximo por imagen. */
    private long maxFileSize = 5L * 1024 * 1024;

    /** Tipos permitidos: solo imágenes. */
    private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    /** Prefijo de las claves de producto dentro del bucket. */
    private String prefix = "products";

    /** Prefijo de las claves de categoría dentro del bucket. */
    private String categoryPrefix = "categories";

    @jakarta.annotation.PostConstruct
    public void validate() {
        if (!enabled) {
            return;
        }
        require(endpoint, "app.storage.b2.endpoint (B2_ENDPOINT)");
        require(region, "app.storage.b2.region (B2_REGION)");
        require(bucket, "app.storage.b2.bucket (B2_BUCKET)");
        require(accessKey, "app.storage.b2.access-key (B2_ACCESS_KEY)");
        require(secretKey, "app.storage.b2.secret-key (B2_SECRET_KEY)");
        require(prefix, "app.storage.b2.prefix (B2_PREFIX)");
        require(categoryPrefix, "app.storage.b2.category-prefix (B2_CATEGORY_PREFIX)");
        if (presignExpiration == null || presignExpiration.isZero() || presignExpiration.isNegative()) {
            throw new IllegalArgumentException("app.storage.b2.presign-expiration debe ser una duración positiva");
        }
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("app.storage.b2.max-file-size debe ser mayor que cero");
        }
    }

    private void require(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta " + property + " con el almacenamiento de imágenes activo");
        }
    }
}