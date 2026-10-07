package com.rtms.backend.service;

import com.rtms.backend.dto.UrgentAssignmentAlert;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.IncidentSeverity;
import com.rtms.backend.enums.TrainingDecision;
import com.rtms.backend.event.UrgentAssignmentCommittedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UrgentAlertStreamServiceTest {

    @Test
    void listenerRunsOnlyAfterTransactionCommit() throws Exception {
        TransactionalEventListener annotation = UrgentAlertStreamService.class
                .getMethod("afterAssignmentCommitted", UrgentAssignmentCommittedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertNotNull(annotation);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
        assertFalse(annotation.fallbackExecution(), "No transaction means no popup event");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsEventOnlyToAssignedVeterinarian() throws Exception {
        UrgentAlertStreamService service = new UrgentAlertStreamService();
        SseEmitter assignedVetEmitter = mock(SseEmitter.class);
        SseEmitter otherVetEmitter = mock(SseEmitter.class);

        Field field = UrgentAlertStreamService.class.getDeclaredField("emittersByVet");
        field.setAccessible(true);
        Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
                (Map<Long, CopyOnWriteArrayList<SseEmitter>>) field.get(service);
        emitters.put(2L, new CopyOnWriteArrayList<>(java.util.List.of(assignedVetEmitter)));
        emitters.put(3L, new CopyOnWriteArrayList<>(java.util.List.of(otherVetEmitter)));

        LocalDateTime now = LocalDateTime.now();
        UrgentAssignmentAlert alert = new UrgentAssignmentAlert(
                500L, 500L, 700L, 2L, 99L, "Rocket", "North barn", "A-01",
                8L, "Groom A", now, IncidentSeverity.CRITICAL, "Acute lameness",
                "Cannot bear weight", null, TrainingDecision.BLOCKED,
                CareScheduleStatus.SCHEDULED, now, now);

        service.afterAssignmentCommitted(new UrgentAssignmentCommittedEvent(alert));

        verify(assignedVetEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(otherVetEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }
}
