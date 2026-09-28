package com.rtms.backend.service;

import com.rtms.backend.dto.GroomAdmissionQueueResponse;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.AdmissionDocumentRepository;
import com.rtms.backend.repository.CandidateHorseProfileRepository;
import com.rtms.backend.repository.HealthRecordRepository;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdmissionQueryServiceTest {
    private AdmissionApplicationRepository admissions;
    private AdmissionQueryService service;

    @BeforeEach
    void setUp() {
        admissions = mock(AdmissionApplicationRepository.class);
        service = new AdmissionQueryService(admissions,
                mock(CandidateHorseProfileRepository.class),
                mock(AdmissionDocumentRepository.class),
                mock(StableStallRepository.class),
                mock(HealthRecordRepository.class),
                mock(AdmissionFileStorage.class),
                mock(UserRepository.class));
    }

    @Test
    void groomQueueTrimsNameUsesInclusiveDateRangeAndCapsPageSize() {
        var page = PageRequest.of(2, 10,
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id")));
        when(admissions.findGroomQueue(eq(false), eq(AdmissionStatus.GROOM_REVIEW), eq(true), eq("Mercury"),
                eq(true), any(), eq(true), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), page, 0));

        GroomAdmissionQueueResponse response = service.getGroomQueue(" Mercury ", null,
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 8), 2, 50);

        assertEquals(10, response.size());
        assertEquals(2, response.page());
        verify(admissions).findGroomQueue(eq(false), eq(AdmissionStatus.GROOM_REVIEW), eq(true), eq("Mercury"),
                eq(true), eq(LocalDateTime.of(2026, 1, 2, 0, 0)),
                eq(true), eq(LocalDateTime.of(2026, 1, 9, 0, 0)), any(Pageable.class));
    }

    @Test
    void groomQueueRejectsReversedDateRangeBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.getGroomQueue("", null,
                LocalDate.of(2026, 1, 9), LocalDate.of(2026, 1, 8), 0, 10));
        verifyNoInteractions(admissions);
    }
}
