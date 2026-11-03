package com.rtms.backend.ai.service;

import java.util.Collections;
import java.util.List;

/**
 * Dịch vụ phân rã và phân loại ý định người dùng (Pre-Flight Intent Router)
 * phục vụ cơ chế Guardrail ở cấp độ điều phối ReAct.
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

    DecomposedIntents decompose(String rawMessage, String role);
}
