package com.rtms.backend.ai.tool.handler;

import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.enums.GroomTaskType;

/** Formats groom task priorities and labels for AI tool results. */
final class GroomTaskPresentation {
    private GroomTaskPresentation() {
    }

    static int getGroomTaskPriority(GroomDailyTask t) {
            if (t == null || t.getTaskType() == null) return 3;
            return switch (t.getTaskType()) {
                case VET_ASSIST, SPECIAL_CARE, HEALTH_CHECK -> 1;
                case WORKOUT_ASSIST -> 2;
                case FEEDING, GROOMING, HOOF_CARE, MUCKING_OUT -> 3;
            };
        }
    
        static String formatGroomTaskType(GroomTaskType type) {
            if (type == null) return "Daily task";
            return switch (type) {
                case FEEDING -> "Feeding (Feeding)";
                case MUCKING_OUT -> "Stall cleaning & bedding replacement (Mucking Out)";
                case GROOMING -> "Brushing & cleaning (Grooming)";
                case HOOF_CARE -> "Hoof care (Hoof Care)";
                case HEALTH_CHECK -> "Vital signs check (Health Check)";
                case WORKOUT_ASSIST -> "Taking the horse to the training area (Workout Assist)";
                case VET_ASSIST -> "Veterinary assistance (Vet Assist)";
                case SPECIAL_CARE -> "Special medical care (Special Care)";
            };
        }
}
