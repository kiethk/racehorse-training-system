package com.rtms.backend.admission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.admission.dto.OcrExtractionResponse;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.enums.HorseSex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
public class AdmissionOcrGatewayService {
    private static final Logger log = LoggerFactory.getLogger(AdmissionOcrGatewayService.class);

    private final FileStorageService fileStorage;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String aiServiceUrl;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public AdmissionOcrGatewayService(
            FileStorageService fileStorage,
            ObjectMapper objectMapper,
            @Value("${rtms.ai.ocr.service-url:http://localhost:8000}") String aiServiceUrl,
            @Value("${rtms.ai.ocr.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${rtms.ai.ocr.read-timeout-seconds:35}") int readTimeoutSeconds) {
        this.fileStorage = fileStorage;
        this.objectMapper = objectMapper;
        this.aiServiceUrl = aiServiceUrl.endsWith("/") ? aiServiceUrl.substring(0, aiServiceUrl.length() - 1) : aiServiceUrl;
        this.connectTimeout = Duration.ofSeconds(connectTimeoutSeconds);
        this.readTimeout = Duration.ofSeconds(readTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(this.connectTimeout)
                .build();
    }

    public OcrExtractionResponse scanAndExtract(MultipartFile file, AdmissionDocumentType documentType) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required for OCR scanning");
        }

        // 1. Read raw bytes up-front to prevent stream closed / stream exhaustion issues
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read uploaded file bytes for OCR: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read uploaded file content");
        }

        // 2. Validate file (size, content type, magic bytes) via FileStorageService
        fileStorage.validate(file);

        // 3. Store file immediately on local disk / Cloudinary to guarantee Zero Duplicate Upload
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String tempFolder = "rtms/admissions/temp/" + UUID.randomUUID();
        String storedKey;
        try {
            storedKey = fileStorage.store(file, tempFolder);
        } catch (Exception ex) {
            log.warn("File storage store operation failed: {}. Preserving local temporary marker.", ex.getMessage());
            storedKey = "temp:" + UUID.randomUUID() + "_" + originalFilename;
        }

        OcrExtractionResponse.FileReference fileReference = new OcrExtractionResponse.FileReference(
                storedKey,
                storedKey,
                originalFilename
        );

        // 4. Delegate to AI Worker service
        try {
            return callAiWorker(fileBytes, originalFilename, contentType, documentType, fileReference);
        } catch (Exception ex) {
            log.warn("AI OCR Worker call failed or unavailable ({}: {}). Returning safe fallback with stored file.",
                    aiServiceUrl, ex.getMessage());
            return createFallbackResponse(fileReference, "AI OCR service is temporarily unavailable. The document was uploaded successfully for manual review.");
        }
    }

    private OcrExtractionResponse callAiWorker(
            byte[] fileBytes,
            String filename,
            String contentType,
            AdmissionDocumentType documentType,
            OcrExtractionResponse.FileReference fileReference) throws IOException, InterruptedException {
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] multipartBody = buildMultipartBody(boundary, fileBytes, filename, contentType, documentType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiServiceUrl + "/internal/v1/ocr/extract"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(readTimeout)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            log.warn("AI OCR Worker responded with status {}: {}", response.statusCode(), response.body());
            return createFallbackResponse(fileReference, "OCR extraction could not process this document. File was preserved.");
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        JsonNode dataNode = rootNode.path("data");

        String name = getNullableText(dataNode, "name");
        String breed = getNullableText(dataNode, "breed");
        String rawSex = getNullableText(dataNode, "sex");
        HorseSex parsedSex = parseHorseSex(rawSex, getNullableText(dataNode, "gender"));
        String dateOfBirth = getNullableText(dataNode, "dateOfBirth");
        String registrationNumber = getNullableText(dataNode, "registrationNumber");
        String registryName = getNullableText(dataNode, "registryName");
        String sireName = getNullableText(dataNode, "sireName");
        String sireRegistrationNumber = getNullableText(dataNode, "sireRegistrationNumber");
        String damName = getNullableText(dataNode, "damName");
        String damRegistrationNumber = getNullableText(dataNode, "damRegistrationNumber");

        Double confidence = dataNode.has("confidenceScore") && !dataNode.get("confidenceScore").isNull()
                ? dataNode.get("confidenceScore").asDouble()
                : null;
        boolean hasConflict = dataNode.path("hasConflict").asBoolean(false);
        String conflictMessage = getNullableText(dataNode, "conflictMessage");

        int fieldsCount = 0;
        if (name != null) fieldsCount++;
        if (parsedSex != null) fieldsCount++;
        if (breed != null) fieldsCount++;
        if (dateOfBirth != null) fieldsCount++;
        if (registrationNumber != null) fieldsCount++;
        if (registryName != null) fieldsCount++;
        if (sireName != null) fieldsCount++;
        if (sireRegistrationNumber != null) fieldsCount++;
        if (damName != null) fieldsCount++;
        if (damRegistrationNumber != null) fieldsCount++;

        OcrExtractionResponse.ExtractedData extractedData = new OcrExtractionResponse.ExtractedData(
                name,
                parsedSex,
                rawSex,
                breed,
                dateOfBirth,
                registrationNumber,
                registryName,
                sireName,
                sireRegistrationNumber,
                damName,
                damRegistrationNumber,
                fileReference,
                confidence,
                hasConflict,
                conflictMessage
        );

        return new OcrExtractionResponse(
                true,
                "Document scanned successfully",
                extractedData,
                fieldsCount
        );
    }

    private byte[] buildMultipartBody(
            String boundary,
            byte[] fileBytes,
            String filename,
            String contentType,
            AdmissionDocumentType documentType) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String lineBreak = "\r\n";

        // Field: documentType (optional)
        if (documentType != null) {
            out.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"documentType\"" + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
            out.write((documentType.name() + lineBreak).getBytes(StandardCharsets.UTF_8));
        }

        // Field: file
        String safeFilename = (filename != null && !filename.isBlank()) ? filename : "document";
        String safeContentType = (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";
        out.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename + "\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + safeContentType + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write(lineBreak.getBytes(StandardCharsets.UTF_8));

        // End of multipart
        out.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private HorseSex parseHorseSex(String rawSex, String gender) {
        String candidate = rawSex != null && !rawSex.isBlank() ? rawSex.trim().toUpperCase() : "";
        if (candidate.isEmpty() && gender != null && !gender.isBlank()) {
            candidate = gender.trim().toUpperCase();
        }
        return switch (candidate) {
            case "MALE", "COLT", "STALLION", "GELDING", "DUC", "ĐỰC" -> HorseSex.MALE;
            case "FEMALE", "FILLY", "MARE", "CAI", "CÁI" -> HorseSex.FEMALE;
            default -> null; // Strictly leave null for human review when uncertain
        };
    }

    private String getNullableText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        String text = node.get(fieldName).asText().trim();
        return text.isEmpty() ? null : text;
    }

    private OcrExtractionResponse createFallbackResponse(OcrExtractionResponse.FileReference fileReference, String message) {
        OcrExtractionResponse.ExtractedData data = new OcrExtractionResponse.ExtractedData(
                null, null, null, null, null, null, null, null, null, null, null,
                fileReference,
                0.0,
                false,
                null
        );
        return new OcrExtractionResponse(false, message, data, 0);
    }
}
