package com.rtms.backend.stable.util;

import com.rtms.backend.stable.enums.AreaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AreaCodeGeneratorTest {

    @Test
    @DisplayName("alphaToIndex and indexToAlpha invert each other accurately")
    void testBijectiveConversion() {
        assertEquals(0, AreaCodeGenerator.alphaToIndex("A"));
        assertEquals(25, AreaCodeGenerator.alphaToIndex("Z"));
        assertEquals(26, AreaCodeGenerator.alphaToIndex("AA"));
        assertEquals(27, AreaCodeGenerator.alphaToIndex("AB"));
        assertEquals(51, AreaCodeGenerator.alphaToIndex("AZ"));
        assertEquals(52, AreaCodeGenerator.alphaToIndex("BA"));

        assertEquals("A", AreaCodeGenerator.indexToAlpha(0));
        assertEquals("Z", AreaCodeGenerator.indexToAlpha(25));
        assertEquals("AA", AreaCodeGenerator.indexToAlpha(26));
        assertEquals("AB", AreaCodeGenerator.indexToAlpha(27));
        assertEquals("AZ", AreaCodeGenerator.indexToAlpha(51));
        assertEquals("BA", AreaCodeGenerator.indexToAlpha(52));
    }

    @Test
    @DisplayName("nextRegularCode correctly generates next letter and reserves Q")
    void testNextRegularCode() {
        // Empty existing -> A
        assertEquals("A", AreaCodeGenerator.nextRegularCode(List.of()));

        // A, B, C, D -> E
        assertEquals("E", AreaCodeGenerator.nextRegularCode(List.of("A", "B", "C", "D")));

        // P (index 15) -> next would be Q (index 16), which is reserved -> skips to R (index 17)
        assertEquals("R", AreaCodeGenerator.nextRegularCode(List.of("A", "B", "P")));

        // Z -> AA
        assertEquals("AA", AreaCodeGenerator.nextRegularCode(List.of("A", "Z")));

        // AA -> AB
        assertEquals("AB", AreaCodeGenerator.nextRegularCode(List.of("A", "AA")));
    }

    @Test
    @DisplayName("nextQuarantineCode correctly generates Q-A, Q-B, etc.")
    void testNextQuarantineCode() {
        // Empty existing -> Q-A
        assertEquals("Q-A", AreaCodeGenerator.nextQuarantineCode(List.of()));

        // Legacy "Q" treated as Q-A -> next is Q-B
        assertEquals("Q-B", AreaCodeGenerator.nextQuarantineCode(List.of("Q")));

        // Q-A -> Q-B
        assertEquals("Q-B", AreaCodeGenerator.nextQuarantineCode(List.of("Q-A")));

        // Q-A, Q-B -> Q-C
        assertEquals("Q-C", AreaCodeGenerator.nextQuarantineCode(List.of("Q-A", "Q-B")));

        // Q-Z -> Q-AA
        assertEquals("Q-AA", AreaCodeGenerator.nextQuarantineCode(List.of("Q-Z")));
    }

    @Test
    @DisplayName("formatStallCode adheres to regular and quarantine conventions")
    void testFormatStallCode() {
        assertEquals("A01", AreaCodeGenerator.formatStallCode(AreaType.REGULAR, "A", 1));
        assertEquals("A09", AreaCodeGenerator.formatStallCode(AreaType.REGULAR, "A", 9));
        assertEquals("A10", AreaCodeGenerator.formatStallCode(AreaType.REGULAR, "A", 10));
        assertEquals("A12", AreaCodeGenerator.formatStallCode(AreaType.REGULAR, "A", 12));
        assertEquals("AA12", AreaCodeGenerator.formatStallCode(AreaType.REGULAR, "AA", 12));
        assertEquals("AA01", AreaCodeGenerator.formatStallCode(AreaType.REGULAR, "AA", 1));

        assertEquals("Q-A01", AreaCodeGenerator.formatStallCode(AreaType.QUARANTINE, "Q-A", 1));
        assertEquals("Q-A12", AreaCodeGenerator.formatStallCode(AreaType.QUARANTINE, "Q-A", 12));
        assertEquals("Q-B07", AreaCodeGenerator.formatStallCode(AreaType.QUARANTINE, "Q-B", 7));
    }

    @Test
    @DisplayName("generateAreaName formats appropriate names")
    void testGenerateAreaName() {
        assertEquals("Area A", AreaCodeGenerator.generateAreaName(AreaType.REGULAR, "A"));
        assertEquals("Area AA", AreaCodeGenerator.generateAreaName(AreaType.REGULAR, "AA"));
        assertEquals("Quarantine Area A", AreaCodeGenerator.generateAreaName(AreaType.QUARANTINE, "Q-A"));
        assertEquals("Quarantine Area B", AreaCodeGenerator.generateAreaName(AreaType.QUARANTINE, "Q-B"));
    }
}
