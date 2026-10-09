package com.rtms.backend.admission.dto;

import com.rtms.backend.admission.enums.ReviewDecision;

public class GroomAdmissionReviewRequest {
    private ReviewDecision decision;
    private String feedback;

    public ReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
