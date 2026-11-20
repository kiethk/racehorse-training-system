package com.rtms.backend.horse.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for strict UELN (Universal Equine Life Number) normalization and validation.
 *
 * Strict 15-character standard:
 * 1. First 3 digits: ISO numeric country code where database exists (e.g. 123, 704)
 * 2. Next 3 digits: Database code within the country (e.g. 001)
 * 3. Final 9 digits: Alphanumeric horse number assigned by breed organization (e.g. 00987654A)
 * Format: ^[0-9]{6}[A-Z0-9]{9}$
 */
public final class UelnUtils {

    public static final String UELN_REGEX = "^[0-9]{6}[A-Z0-9]{9}$";
    public static final Pattern UELN_PATTERN = Pattern.compile(UELN_REGEX);

    private UelnUtils() {
    }

    /**
     * Strips whitespace, hyphens, underscores, dots, and converts to uppercase.
     * Empty string or whitespace-only is converted to null (UELN may be unknown).
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\s\\-_.]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.toUpperCase(Locale.ROOT);
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
                    fieldLabel + " must be a valid 15-character UELN with 6 digits followed by 9 alphanumeric characters (e.g. 12300100987654A or 704001000000001)"
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
