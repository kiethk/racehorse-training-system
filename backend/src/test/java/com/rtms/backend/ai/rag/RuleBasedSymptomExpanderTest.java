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

        // Khởi tạo fixture dữ liệu giả lập từ Database (100% data-driven)
        List<MedicalSymptomMapping> testMappings = List.of(
                new MedicalSymptomMapping("cào đất", "đau bụng cấp tính ở ngựa (equine colic)", "DIGESTIVE"),
                new MedicalSymptomMapping("quay đầu nhìn sườn", "đau bụng cấp tính (colic)", "DIGESTIVE"),
                new MedicalSymptomMapping("nằm lăn", "đau bụng cấp dữ dội (severe colic)", "DIGESTIVE"),
                new MedicalSymptomMapping("tập tễnh", "què/khập khiễng (equine lameness)", "LOCOMOTOR"),
                new MedicalSymptomMapping("khập khiễng", "què/khập khiễng (equine lameness)", "LOCOMOTOR"),
                new MedicalSymptomMapping("nóng móng", "viêm màng đáy móng (laminitis)", "LOCOMOTOR"),
                new MedicalSymptomMapping("sưng khớp", "viêm khớp (arthritis / synovitis)", "LOCOMOTOR"),
                new MedicalSymptomMapping("ho", "hen suyễn hoặc cảm cúm ngựa", "RESPIRATORY")
        );

        when(mockRepository.findByIsActiveTrue()).thenReturn(testMappings);

        expander = new RuleBasedSymptomExpander(mockRepository);
        expander.reload();
    }

    @Test
    @DisplayName("Đối soát triệu chứng đau bụng cấp (Colic) từ dữ liệu DB nạp vào Cache")
    void testColicSymptomsExpansion() {
        String input = "Chiến mã H-001 sáng nay liên tục cào đất và hay quay đầu nhìn sườn, có lúc nằm lăn ra chuồng.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertFalse(terms.isEmpty(), "Phải trích xuất được thuật ngữ y khoa");
        assertTrue(terms.stream().anyMatch(t -> t.contains("colic")), "Phải nhận diện được hội chứng đau bụng cấp Colic");

        String expanded = expander.expand(input);
        assertTrue(expanded.contains("[Thuật ngữ y khoa liên quan:"));
        assertTrue(expanded.contains("equine colic"));
    }

    @Test
    @DisplayName("Đối soát triệu chứng què/khập khiễng và sưng khớp từ DB Cache")
    void testLamenessSymptomsExpansion() {
        String input = "Ngựa đi lại tập tễnh sau buổi tập sáng, chân sau sưng khớp và nóng móng.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertTrue(terms.stream().anyMatch(t -> t.contains("lameness") || t.contains("khập khiễng")));
        assertTrue(terms.stream().anyMatch(t -> t.contains("laminitis")));
        assertTrue(terms.stream().anyMatch(t -> t.contains("arthritis") || t.contains("viêm khớp")));
    }

    @Test
    @DisplayName("Không thêm thuật ngữ nếu văn bản không chứa triệu chứng bệnh (Tránh bắt nhầm chuỗi con 'ho' trong 'hoàn thành')")
    void testNoSymptomsFound() {
        String input = "Ngựa hôm nay hoàn thành tốt bài tập cự ly 1600m trên mặt sân cỏ.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertTrue(terms.isEmpty());
        assertEquals(input, expander.expand(input));
    }

    @Test
    @DisplayName("Cập nhật In-Memory Cache tức thì khi reload() dữ liệu mới từ Database")
    void testDynamicReloadFromDatabase() {
        // Giả lập DB có thêm triệu chứng mới được Bác sĩ thú y thêm qua giao diện web
        MedicalSymptomMapping customMapping = new MedicalSymptomMapping(
                "mắt lờ đờ", "viêm màng bồ đào ngựa (equine recurrent uveitis / moon blindness)", "OPHTHALMIC");
        when(mockRepository.findByIsActiveTrue()).thenReturn(List.of(customMapping));

        // Gọi reload() để cập nhật lại cache
        expander.reload();

        String input = "Ngựa có dấu hiệu mỏi mệt, mắt lờ đờ không chịu nhìn.";
        Set<String> terms = expander.extractMedicalTerms(input);

        assertFalse(terms.isEmpty());
        assertTrue(terms.stream().anyMatch(t -> t.contains("uveitis") || t.contains("bồ đào")));
    }
}
