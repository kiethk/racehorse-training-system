package com.rtms.backend.medical.service;

import java.util.Locale;
import java.util.Set;

public final class TreatmentRoutes {
    private static final Set<String> ALLOWED_ROUTES = Set.of("ORAL", "TOPICAL", "FEED_ADDITIVE");

    private TreatmentRoutes() {
    }

    public static boolean isAllowedTreatmentRoute(String route) {
        return route != null && ALLOWED_ROUTES.contains(route.trim().toUpperCase(Locale.ROOT));
    }
}
