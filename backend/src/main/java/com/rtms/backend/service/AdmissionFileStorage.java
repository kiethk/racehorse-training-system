package com.rtms.backend.service;

import com.rtms.backend.entity.AdmissionDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Private local file storage for development/single-instance deployment.
 * Configure RTMS_ADMISSION_UPLOAD_DIR as a persistent private volume.
 * Replace this component with an R2/S3 implementation for multi-instance deployment.
 */
@Component
public class AdmissionFileStorage {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");
    private final Path uploadRoot;

    public AdmissionFileStorage(@Value("${rtms.admission.upload-dir:./uploads/admissions}") String directory) {
        this.uploadRoot = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be between 1 byte and 10 MB");
        }
        String mediaType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(mediaType);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPEG, PNG and WebP are accepted");
        }
        String storageKey = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadRoot);
            Path destination = uploadRoot.resolve(storageKey);
            try (var stream = file.getInputStream()) {
                Files.copy(stream, destination, StandardCopyOption.CREATE_NEW);
            }
            return "local:" + storageKey;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store admission document");
        }
    }

    public void deleteIfLocal(String key) {
        if (key == null || !key.startsWith("local:")) return;
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
            // A cleanup failure must not hide the original persistence error.
        }
    }

    public Resource load(String key) {
        if (key == null || !key.startsWith("local:")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This document is hosted externally");
        }
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
        }
        return new FileSystemResource(path);
    }

    private Path resolve(String key) {
        String filename = key.substring("local:".length());
        if (!filename.matches("[a-f0-9\\-]{36}\\.(pdf|jpg|png|webp)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document key");
        }
        Path result = uploadRoot.resolve(filename).normalize();
        if (!result.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document path");
        }
        return result;
    }

    public String downloadUrl(AdmissionDocument document) {
        if (document.getFileUrl().startsWith("local:")) {
            return "/api/admissions/" + document.getAdmissionId() + "/documents/"
                    + document.getId() + "/file";
        }
        return document.getFileUrl();
    }
}
