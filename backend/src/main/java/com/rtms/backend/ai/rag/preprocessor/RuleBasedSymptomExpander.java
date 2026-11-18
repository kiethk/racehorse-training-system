package com.rtms.backend.ai.rag.preprocessor;

import com.rtms.backend.ai.rag.entity.MedicalSymptomMapping;
import com.rtms.backend.ai.rag.repository.MedicalSymptomMappingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Preprocesses and expands clinical symptoms using the database and an in-memory cache.
 *
 * All symptom data is managed entirely in the database (medical_symptom_mappings table),
 * with no hardcoded entries in the source code.
 *
 * At startup (@PostConstruct) or when reload() is called, the system loads data from the database into memory
 * with precompiled Unicode word-boundary regex patterns, ensuring AI response times < 0.1ms.
 */
@Component
public class RuleBasedSymptomExpander {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedSymptomExpander.class);

    private final MedicalSymptomMappingRepository repository;

    private final Map<String, List<String>> symptomToMedicalTerms = new ConcurrentHashMap<>();
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    public RuleBasedSymptomExpander(MedicalSymptomMappingRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * Reloads the entire symptom dictionary from the database into the in-memory cache.
     */
    public synchronized void reload() {
        symptomToMedicalTerms.clear();
        compiledPatterns.clear();

        List<MedicalSymptomMapping> dbMappings = repository.findByIsActiveTrue();
        if (dbMappings != null && !dbMappings.isEmpty()) {
            for (MedicalSymptomMapping mapping : dbMappings) {
                registerSingle(mapping.getRawSymptom(), mapping.getMedicalTerm());
            }
            log.info("RuleBasedSymptomExpander: Successfully loaded {} medical symptoms from the database into the in-memory cache.", dbMappings.size());
        } else {
            log.warn("RuleBasedSymptomExpander: The medical_symptom_mappings table is empty or has no active records.");
        }
    }

    private void registerSingle(String rawSymptom, String medicalTerm) {
        if (rawSymptom == null || rawSymptom.isBlank() || medicalTerm == null || medicalTerm.isBlank()) {
            return;
        }
        String key = rawSymptom.toLowerCase(Locale.ROOT).trim();
        symptomToMedicalTerms.computeIfAbsent(key, k -> new ArrayList<>()).add(medicalTerm.trim());

        // Unicode word-boundary regex: (?<!\p{L}) and (?!\p{L}) to avoid matching substrings
        Pattern pattern = Pattern.compile("(?<!\\p{L})" + Pattern.quote(key) + "(?!\\p{L})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        compiledPatterns.put(key, pattern);
    }

    /**
     * Extracts the corresponding medical terms from symptom description text.
     *
     * @param rawText Input text (for example: "The horse paws at the ground and often turns to look at its belly")
     * @return Set of mapped specialized medical terms
     */
    public Set<String> extractMedicalTerms(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> matchedTerms = new LinkedHashSet<>();

        for (Map.Entry<String, Pattern> entry : compiledPatterns.entrySet()) {
            if (entry.getValue().matcher(rawText).find()) {
                List<String> terms = symptomToMedicalTerms.get(entry.getKey());
                if (terms != null) {
                    matchedTerms.addAll(terms);
                }
            }
        }

        return matchedTerms;
    }

    /**
     * Expands the input text by preserving the original question or symptom and adding
     * related medical terms to optimize vector semantic search.
     *
     * @param rawText Original question or symptom note
     * @return Text enriched with medical context
     */
    public String expand(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return rawText;
        }

        Set<String> medicalTerms = extractMedicalTerms(rawText);
        if (medicalTerms.isEmpty()) {
            return rawText;
        }

        StringBuilder sb = new StringBuilder(rawText.trim());
        sb.append(" [Related medical terms: ");
        sb.append(String.join(", ", medicalTerms));
        sb.append("]");

        return sb.toString();
    }

    /**
     * Gets the entire mapping dictionary in the cache (read-only view for testing and auditing).
     */
    public Map<String, List<String>> getDictionary() {
        return Collections.unmodifiableMap(symptomToMedicalTerms);
    }
}
