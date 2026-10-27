package com.rtms.backend.admission.repository;

import com.rtms.backend.admission.enums.AdmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class GroomQueueRepositoryTest {
    @Autowired
    private AdmissionApplicationRepository admissions;

    @Test
    void filteredQueueQueryExecutesAgainstPostgres() {
        var result = admissions.findGroomQueue(-1L, false, true, LocalDateTime.of(2026, 1, 1, 0, 0),
                AdmissionStatus.GROOM_REVIEW,
                true, AdmissionStatus.GROOM_REVIEW,
                true, "__groom_queue_query_smoke__", true, LocalDateTime.of(1900, 1, 1, 0, 0),
                true, LocalDateTime.of(1901, 1, 1, 0, 0),
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))));

        assertEquals(10, result.getSize());
    }
}
