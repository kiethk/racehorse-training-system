package com.rtms.backend.ai.service;

import com.rtms.backend.ai.dto.AiChatMessageDto;

import java.util.Collections;
import java.util.List;

/**
 * Service for decomposing and classifying user intent (Pre-Flight Intent Router),
 * supporting the Guardrail mechanism at the ReAct orchestration level.
 */
public interface IntentDecompositionService {

    record DecomposedIntents(
            List<String> inDomainIntents,
            List<String> outOfDomainIntents
    ) {
        public DecomposedIntents {
            inDomainIntents = inDomainIntents != null ? inDomainIntents : Collections.emptyList();
            outOfDomainIntents = outOfDomainIntents != null ? outOfDomainIntents : Collections.emptyList();
        }

        public boolean hasInDomain() {
            return !inDomainIntents.isEmpty();
        }

        public boolean hasOutOfDomain() {
            return !outOfDomainIntents.isEmpty();
        }

        public boolean isPureOutOfDomain() {
            return !hasInDomain() && hasOutOfDomain();
        }

        public boolean isPureInDomain() {
            return hasInDomain() && !hasOutOfDomain();
        }
    }

    default DecomposedIntents decompose(String rawMessage, String role) {
        return decompose(rawMessage, role, Collections.emptyList());
    }

    DecomposedIntents decompose(String rawMessage, String role, List<AiChatMessageDto> conversationHistory);
}

