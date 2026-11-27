package com.angel.ecomerce.dto.storage;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Contenido de una imagen lista para subir al bucket.
 *
 * <p>Se construye en el controlador desde el {@code MultipartFile} del request;
 * los servicios no dependen de tipos web.</p>
 */
public record ImageUpload(byte[] content, String contentType, String filename) {

    public static ImageUpload of(byte[] content, String contentType, String filename) {
        return new ImageUpload(content, contentType, filename);
    }

    public long size() {
        return content == null ? 0 : content.length;
    }

    /** Extensión derivada del tipo de contenido (imagen, así que no hay confianza en el nombre). */
    public String extension() {
        if (contentType == null) {
            return "bin";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
    }

    public String contentTypeOrDefault() {
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
    }

    public boolean hasContent() {
        return content != null && content.length > 0;
    }

    public String filenameOr(String fallback) {
        return filename == null || filename.isBlank() ? fallback : filename;
    }

    /** Nombre saneado por si alguien manda un nombre con rutas o caracteres raros. */
    public String safeName() {
        String base = filenameOr("imagen").replaceAll("[^a-zA-Z0-9-_]", "-");
        return base.isEmpty() ? "imagen" : base.substring(0, Math.min(base.length(), 64));
    }

    public String contentAsString() {
        return content == null ? "" : new String(content, StandardCharsets.UTF_8);
    }
}