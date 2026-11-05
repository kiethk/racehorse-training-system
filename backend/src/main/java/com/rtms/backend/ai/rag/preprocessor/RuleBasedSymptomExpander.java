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
 * Tiền xử lý mở rộng triệu chứng lâm sàng dựa trên Database + In-Memory Cache.
 *
 * Toàn bộ dữ liệu triệu chứng được quản trị 100% trong Database (bảng medical_symptom_mappings),
 * hoàn toàn không hardcode trong mã nguồn.
 *
 * Lúc khởi động (@PostConstruct) hoặc khi gọi reload(), hệ thống nạp dữ liệu từ DB vào RAM
 * với các mẫu Regex biên từ Unicode đã biên dịch sẵn, đảm bảo tốc độ phản hồi < 0.1ms cho AI.
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
     * Tải lại toàn bộ từ điển triệu chứng từ cơ sở dữ liệu vào In-Memory Cache.
     */
    public synchronized void reload() {
        symptomToMedicalTerms.clear();
        compiledPatterns.clear();

        List<MedicalSymptomMapping> dbMappings = repository.findByIsActiveTrue();
        if (dbMappings != null && !dbMappings.isEmpty()) {
            for (MedicalSymptomMapping mapping : dbMappings) {
                registerSingle(mapping.getRawSymptom(), mapping.getMedicalTerm());
            }
            log.info("RuleBasedSymptomExpander: Đã nạp thành công {} triệu chứng y khoa từ Database vào In-Memory Cache.", dbMappings.size());
        } else {
            log.warn("RuleBasedSymptomExpander: Bảng medical_symptom_mappings đang rỗng hoặc chưa kích hoạt bản ghi nào.");
        }
    }

    private void registerSingle(String rawSymptom, String medicalTerm) {
        if (rawSymptom == null || rawSymptom.isBlank() || medicalTerm == null || medicalTerm.isBlank()) {
            return;
        }
        String key = rawSymptom.toLowerCase(Locale.ROOT).trim();
        symptomToMedicalTerms.computeIfAbsent(key, k -> new ArrayList<>()).add(medicalTerm.trim());

        // Regex ranh giới từ theo chuẩn Unicode: (?<!\p{L}) và (?!\p{L}) để tránh match chuỗi con
        Pattern pattern = Pattern.compile("(?<!\\p{L})" + Pattern.quote(key) + "(?!\\p{L})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        compiledPatterns.put(key, pattern);
    }

    /**
     * Trích xuất các thuật ngữ y khoa tương ứng từ văn bản mô tả triệu chứng.
     *
     * @param rawText Văn bản đầu vào (ví dụ: "Ngựa có biểu hiện cào đất và hay quay đầu nhìn bụng")
     * @return Tập hợp các thuật ngữ y học chuyên ngành được ánh xạ
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
     * Mở rộng văn bản đầu vào: giữ nguyên câu hỏi/triệu chứng gốc và bổ sung
     * các thuật ngữ y khoa liên quan để tối ưu hóa vector semantic search.
     *
     * @param rawText Câu hỏi hoặc ghi chú triệu chứng ban đầu
     * @return Văn bản đã được làm giàu ngữ nghĩa y học
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
        sb.append(" [Thuật ngữ y khoa liên quan: ");
        sb.append(String.join(", ", medicalTerms));
        sb.append("]");

        return sb.toString();
    }

    /**
     * Lấy toàn bộ từ điển ánh xạ trong Cache (Read-only view phục vụ kiểm thử / kiểm toán).
     */
    public Map<String, List<String>> getDictionary() {
        return Collections.unmodifiableMap(symptomToMedicalTerms);
    }
}
