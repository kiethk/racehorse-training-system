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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


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

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be between 1 byte and 10 MB");
        }

        String mediaType = file.getContentType() == null ? "" :  file.getContentType().toLowerCase(Locale.ROOT);

        if (!EXTENSIONS.containsKey(mediaType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPEG, PNG and WebP are accepted");
        }

    }

    public String store(MultipartFile file) {
        validate(file);
        String mediaType = file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(mediaType);
        String storageKey = UUID.randomUUID() + extension;
        String key = "local:" + storageKey;
        try {
            Files.createDirectories(uploadRoot);
            Path destination = uploadRoot.resolve(storageKey);
            try (var stream = file.getInputStream()) {
                Files.copy(stream, destination);
            }
            return key;
        } catch (IOException ex) {
            deleteIfLocal(key);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store admission document", ex);
        } catch (RuntimeException ex) {
            deleteIfLocal(key);
            throw ex;
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
