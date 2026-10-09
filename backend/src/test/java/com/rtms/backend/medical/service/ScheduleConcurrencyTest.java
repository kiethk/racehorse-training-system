package com.rtms.backend.medical.service;

import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleConcurrencyTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private CareScheduleService careScheduleService;

    @Test
    @DisplayName("startCareSchedule does not deadlock concurrently and honors finite timeout")
    void testConcurrentStartCareSchedule_noDeadlock() throws Exception {
        Long scheduleId = 100L;
        Long vetId = 200L;
        Long horseId = 300L;

        User vet = new User();
        vet.setId(vetId);

        Horse horse = new Horse();
        horse.setId(horseId);

        CareSchedule schedule = new CareSchedule();
        schedule.setId(scheduleId);
        schedule.setHorseId(horseId);
        schedule.setVeterinarianId(vetId);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);

        when(userRepository.findById(vetId)).thenReturn(Optional.of(vet));
        when(careScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(horseRepository.findByIdForUpdate(horseId)).thenReturn(Optional.of(horse));
        when(careScheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
        when(careScheduleRepository.save(any(CareSchedule.class))).thenAnswer(i -> i.getArgument(0));

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    careScheduleService.startCareSchedule(scheduleId, vetId);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(completed, "Concurrent operations must complete within finite timeout without deadlock");
        assertTrue(successCount.get() >= 1, "At least one start operation succeeded");
    }
}
