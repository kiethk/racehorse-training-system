package com.rtms.backend.stable.util;

import com.rtms.backend.stable.enums.AreaType;

import java.util.Collection;
import java.util.Locale;

public final class AreaCodeGenerator {

    private AreaCodeGenerator() {
    }

    /**
     * Converts a 1-based, base-26 bijective letter sequence ("A", "B", ..., "Z", "AA", "AB")
     * into a zero-based integer index.
     * "A" -> 0, "B" -> 1, ..., "Z" -> 25, "AA" -> 26.
     */
    public static int alphaToIndex(String letters) {
        if (letters == null || letters.isEmpty()) {
            throw new IllegalArgumentException("Letters cannot be null or empty");
        }
        int index = 0;
        for (int i = 0; i < letters.length(); i++) {
            char c = Character.toUpperCase(letters.charAt(i));
            if (c < 'A' || c > 'Z') {
                throw new IllegalArgumentException("Invalid letter in code: " + c);
            }
            index = index * 26 + (c - 'A' + 1);
        }
        return index - 1;
    }

    /**
     * Converts a zero-based integer index into a base-26 bijective letter sequence.
     * 0 -> "A", 1 -> "B", ..., 25 -> "Z", 26 -> "AA".
     */
    public static String indexToAlpha(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be >= 0: " + index);
        }
        StringBuilder sb = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    /**
     * Allocates the next sequential code for a REGULAR area: A-Z, AA-...
     * Reserving 'Q' (never allocated for regular areas).
     */
    public static String nextRegularCode(Collection<String> existingCodes) {
        int maxIndex = -1;
        if (existingCodes != null) {
            for (String code : existingCodes) {
                if (code == null || code.isBlank()) continue;
                String clean = code.trim().toUpperCase();
                // Ensure only pure alphabetical regular codes are parsed
                if (clean.matches("^[A-Z]+$")) {
                    try {
                        int idx = alphaToIndex(clean);
                        if (idx > maxIndex) {
                            maxIndex = idx;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        int nextIndex = maxIndex + 1;
        if ("Q".equals(indexToAlpha(nextIndex))) {
            nextIndex++;
        }
        return indexToAlpha(nextIndex);
    }

    /**
     * Allocates the next sequential code for a QUARANTINE area: Q-A, Q-B..Q-Z, Q-AA..
     */
    public static String nextQuarantineCode(Collection<String> existingCodes) {
        int maxIndex = -1;
        if (existingCodes != null) {
            for (String code : existingCodes) {
                if (code == null || code.isBlank()) continue;
                String clean = code.trim().toUpperCase();
                String suffix = null;
                if (clean.startsWith("Q-")) {
                    suffix = clean.substring(2);
                } else if ("Q".equals(clean)) {
                    // Legacy 'Q' area counts as 'A' (index 0)
                    suffix = "A";
                }
                if (suffix != null && suffix.matches("^[A-Z]+$")) {
                    try {
                        int idx = alphaToIndex(suffix);
                        if (idx > maxIndex) {
                            maxIndex = idx;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        int nextIndex = maxIndex + 1;
        return "Q-" + indexToAlpha(nextIndex);
    }

    /**
     * Generates a human-readable area name.
     */
    public static String generateAreaName(AreaType type, String areaCode) {
        if (type == AreaType.QUARANTINE) {
            String suffix = areaCode.startsWith("Q-") ? areaCode.substring(2) : areaCode;
            return "Quarantine Area " + suffix;
        }
        return "Area " + areaCode;
    }

    /**
     * Generates stall code according to rules:
     * Regular: A01..A12, E01..E12
     * Quarantine: Q-A01..Q-A12
     */
    public static String formatStallCode(AreaType type, String areaCode, int stallNumber) {
        return String.format(Locale.ROOT, "%s%02d", areaCode, stallNumber);
    }
}
