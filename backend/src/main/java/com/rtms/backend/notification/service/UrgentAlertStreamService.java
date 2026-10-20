package com.rtms.backend.notification.service;

import com.rtms.backend.notification.dto.UrgentAssignmentAlert;
import com.rtms.backend.event.UrgentAssignmentCommittedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UrgentAlertStreamService {

    private static final long STREAM_TIMEOUT_MS = 30L * 60L * 1000L;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByVet = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long veterinarianId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emittersByVet.computeIfAbsent(veterinarianId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> remove(veterinarianId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            cleanup.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterAssignmentCommitted(UrgentAssignmentCommittedEvent event) {
        UrgentAssignmentAlert alert = event.alert();
        List<SseEmitter> emitters = emittersByVet.getOrDefault(
                alert.veterinarianId(), new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(alert.eventId()))
                        .name("urgent-assigned")
                        .data(alert));
            } catch (IOException | IllegalStateException ex) {
                remove(alert.veterinarianId(), emitter);
                emitter.complete();
            }
        }
    }

    private void remove(Long veterinarianId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByVet.get(veterinarianId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) emittersByVet.remove(veterinarianId);
    }
}
