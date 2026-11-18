package com.rtms.backend.ai.rag;

import com.rtms.backend.ai.rag.entity.MedicalSymptomMapping;
import com.rtms.backend.ai.rag.preprocessor.RuleBasedSymptomExpander;
import com.rtms.backend.ai.rag.repository.MedicalSymptomMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RuleBasedSymptomExpanderTest {

    private RuleBasedSymptomExpander expander;
    private MedicalSymptomMappingRepository mockRepository;

    @BeforeEach
    void setUp() {
        mockRepository = Mockito.mock(MedicalSymptomMappingRepository.class);

        // Initialize a database fixture (100% data-driven).
        List<MedicalSymptomMapping> testMappings = List.of(
                new MedicalSymptomMapping("paws at the ground", "equine colic", "DIGESTIVE"),
                new MedicalSymptomMapping("turns to look at its flank", "colic", "DIGESTIVE"),
                new MedicalSymptomMapping("rolls on the ground", "severe colic", "DIGESTIVE"),
                new MedicalSymptomMapping("limping", "equine lameness", "LOCOMOTOR"),
                new MedicalSymptomMapping("lameness", "equine lameness", "LOCOMOTOR"),
                new MedicalSymptomMapping("hot hooves", "laminitis", "LOCOMOTOR"),
                new MedicalSymptomMapping("swollen joints", "arthritis / synovitis", "LOCOMOTOR"),
                new MedicalSymptomMapping("pain", "musculoskeletal pain", "LOCOMOTOR")
        );

        when(mockRepository.findByIsActiveTrue()).thenReturn(testMappings);

        expander = new RuleBasedSymptomExpander(mockRepository);
        expander.reload();
    }

    @Test
    @DisplayName("Resolve acute colic symptoms from data loaded into the database cache")
    void testColicSymptomsExpansion() {
        String input = "Racehorse H-001 paws at the ground and turns to look at its flank this morning, and sometimes rolls on the ground in the stall.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertFalse(terms.isEmpty(), "A medical term should be extracted.");
        assertTrue(terms.stream().anyMatch(t -> t.contains("colic")), "Acute colic should be recognized.");

        String expanded = expander.expand(input);
        assertTrue(expanded.contains("[Related medical terms:"));
        assertTrue(expanded.contains("equine colic"));
    }

    @Test
    @DisplayName("Resolve lameness and swollen-joint symptoms from the database cache")
    void testLamenessSymptomsExpansion() {
        String input = "The horse is limping after the morning workout, with swollen joints and hot hooves in its hind legs.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertTrue(terms.stream().anyMatch(t -> t.contains("lameness")));
        assertTrue(terms.stream().anyMatch(t -> t.contains("laminitis")));
        assertTrue(terms.stream().anyMatch(t -> t.contains("arthritis")));
    }

    @Test
    @DisplayName("Do not match a symptom term inside an unrelated word ('pain' inside 'painted')")
    void testNoSymptomsFound() {
        String input = "The horse completed its 1600 m grass-track workout while the stable gate was being painted.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertTrue(terms.isEmpty());
        assertEquals(input, expander.expand(input));
    }

    @Test
    @DisplayName("Immediately update the in-memory cache when reload() receives new database data")
    void testDynamicReloadFromDatabase() {
        // Simulate a new symptom added by a veterinarian through the web interface.
        MedicalSymptomMapping customMapping = new MedicalSymptomMapping(
                "glassy eyes", "equine recurrent uveitis / moon blindness", "OPHTHALMIC");
        when(mockRepository.findByIsActiveTrue()).thenReturn(List.of(customMapping));

        // Reload the cache.
        expander.reload();

        String input = "The horse appears fatigued and has glassy eyes.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertFalse(terms.isEmpty());
        assertTrue(terms.stream().anyMatch(t -> t.contains("uveitis")));
    }
}
