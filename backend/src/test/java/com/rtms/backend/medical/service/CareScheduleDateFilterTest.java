package com.rtms.backend.medical.service;

import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareScheduleDateFilterTest {

    @Mock
    private CareScheduleRepository careScheduleRepository;

    private CareScheduleService careScheduleService;

    @Captor
    private ArgumentCaptor<LocalDateTime> dayStartCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> dayEndCaptor;

    @BeforeEach
    void setUp() {
        careScheduleService = new CareScheduleService(
                careScheduleRepository,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Test
    void listSchedules_withDate_computesDayBoundariesAndPassesToRepository() {
        LocalDate filterDate = LocalDate.of(2026, 10, 18);
        Pageable pageable = PageRequest.of(0, 10);
        Page<CareSchedule> emptyPage = new PageImpl<>(List.of());

        when(careScheduleRepository.findFiltered(
                eq(CareScheduleStatus.SCHEDULED),
                eq(CareType.ROUTINE),
                eq(1L),
                eq(2L),
                eq(3L),
                any(),
                any(),
                eq(pageable)
        )).thenReturn(emptyPage);

        careScheduleService.listSchedules(
                CareScheduleStatus.SCHEDULED,
                CareType.ROUTINE,
                1L,
                2L,
                3L,
                filterDate,
                pageable
        );

        verify(careScheduleRepository).findFiltered(
                eq(CareScheduleStatus.SCHEDULED),
                eq(CareType.ROUTINE),
                eq(1L),
                eq(2L),
                eq(3L),
                dayStartCaptor.capture(),
                dayEndCaptor.capture(),
                eq(pageable)
        );

        LocalDateTime expectedStart = LocalDateTime.of(2026, 10, 18, 0, 0, 0);
        LocalDateTime expectedEnd = LocalDateTime.of(2026, 10, 19, 0, 0, 0);

        assertThat(dayStartCaptor.getValue()).isEqualTo(expectedStart);
        assertThat(dayEndCaptor.getValue()).isEqualTo(expectedEnd);
    }

    @Test
    void listSchedules_withoutDate_passesNullBoundariesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CareSchedule> emptyPage = new PageImpl<>(List.of());

        when(careScheduleRepository.findFiltered(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(pageable)
        )).thenReturn(emptyPage);

        careScheduleService.listSchedules(
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        verify(careScheduleRepository).findFiltered(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                dayStartCaptor.capture(),
                dayEndCaptor.capture(),
                eq(pageable)
        );

        assertThat(dayStartCaptor.getValue()).isNull();
        assertThat(dayEndCaptor.getValue()).isNull();
    }
}
