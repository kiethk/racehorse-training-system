package com.rtms.backend.horse.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for strict UELN (Universal Equine Life Number) normalization and validation.
 *
 * Supports 15-character alphanumeric lifetime codes:
 * 1. Numeric ISO code prefix: Exactly 6 digits + 9 alphanumeric characters (e.g. 704001SC0000001)
 * 2. Alpha-3 ISO country code prefix: 3 letters + 12 alphanumeric characters (e.g. VNM22SC00000001)
 * Format: ^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$
 */
public final class UelnUtils {

    public static final String UELN_REGEX = "^([0-9]{6}[A-Z0-9]{9}|[A-Z]{3}[A-Z0-9]{12})$";
    public static final Pattern UELN_PATTERN = Pattern.compile(UELN_REGEX);

    private UelnUtils() {
    }

    /**
     * Trims leading/trailing whitespace and converts to uppercase.
     * Empty string or whitespace-only is converted to null (UELN may be unknown).
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * Returns true if normalized UELN is null (unknown) or matches the standard 15-char structure.
     */
    public static boolean isValid(String normalized) {
        if (normalized == null) {
            return true;
        }
        return UELN_PATTERN.matcher(normalized).matches();
    }

    /**
     * Normalizes and validates raw UELN input. Throws 400 Bad Request if invalid.
     */
    public static String cleanAndValidate(String raw, String fieldLabel) {
        String normalized = normalize(raw);
        if (normalized != null && !isValid(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldLabel + " must be a valid 15-character UELN (e.g. 704001SC0000001 or VNM22SC00000001)"
            );
        }
        return normalized;
    }

    /**
     * Validates pedigree integrity between child, sire, and dam UELNs.
     * Chặn: child UELN = sire UELN, child = dam, sire = dam khi cả hai có giá trị.
     */
    public static void validatePedigreeUelns(String childUeln, String sireUeln, String damUeln) {
        if (childUeln != null && sireUeln != null && childUeln.equals(sireUeln)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Child UELN and sire UELN cannot be the same"
            );
        }
        if (childUeln != null && damUeln != null && childUeln.equals(damUeln)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Child UELN and dam UELN cannot be the same"
            );
        }
        if (sireUeln != null && damUeln != null && sireUeln.equals(damUeln)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sire UELN and dam UELN cannot be the same"
            );
        }
    }
}
