package com.angel.ecomerce.service.storage;

import com.angel.ecomerce.config.StorageProperties;
import com.angel.ecomerce.dto.storage.ImageUpload;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.net.URI;

/**
 * Cliente real contra Backblaze B2 usando la API compatible con S3 de AWS SDK v2.
 *
 * <p>Detalles que evitan sorpresas con B2:</p>
 * <ul>
 *   <li>El endpoint y la región se configuran a mano: B2 no es AWS y no se
 *       descubren solos.</li>
 *   <li>B2 rechaza la cabecera {@code x-amz-sdk-checksum-algorithm}, así que el
 *       cálculo de checksums se limita a {@code WHEN_REQUIRED}.</li>
 *   <li>El bucket es privado: las lecturas se sirven con URL firmada.</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(prefix = "app.storage.b2", name = "enabled", havingValue = "true")
public class B2ImageStorage implements IImageStorage {

    private static final Logger log = LoggerFactory.getLogger(B2ImageStorage.class);

    private final StorageProperties properties;
    private final S3Client client;
    private final S3Presigner presigner;

    public B2ImageStorage(StorageProperties properties) {
        this.properties = properties;
        this.client = S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .build();
    }

    @Override
    public String store(String key, ImageUpload upload) {
        if (upload == null || !upload.hasContent()) {
            throw new IllegalArgumentException("La imagen está vacía");
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(upload.contentTypeOrDefault())
                .build();
        try {
            client.putObject(request, RequestBody.fromBytes(upload.content()));
            log.debug("Imagen guardada en B2 con clave {}", key);
            return key;
        } catch (SdkException e) {
            throw new IllegalStateException("No se pudo subir la imagen al bucket: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (SdkException e) {
            // Borrar una imagen que ya no está no debe romper la operación de negocio.
            log.warn("No se pudo borrar la imagen {} del bucket: {}", key, e.getMessage());
        }
    }

    @Override
    public String presignedUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(properties.getPresignExpiration())
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .build())
                    .build()).toString();
        } catch (SdkException e) {
            log.warn("No se pudo firmar la URL de la imagen {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @PreDestroy
    public void close() {
        client.close();
        presigner.close();
    }

    private void validate(ImageUpload upload) {
        if (upload == null || !upload.hasContent()) {
            throw new IllegalArgumentException("La imagen está vacía");
        }
    }
}