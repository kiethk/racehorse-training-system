package com.rtms.backend.horse.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.rtms.backend.horse.entity.Horse;

/**
 * API response DTO for horse endpoints.
 * Includes all Horse fields (unwrapped) plus an optional imageUrl.
 * imageUrl points to GET /api/horses/{id}/photo — a protected endpoint
 * that requires HORSE_VIEW and streams the file; null when no photo exists.
 */
public class HorseResponse {

    @JsonUnwrapped
    private final Horse horse;

    private final String imageUrl;

    public HorseResponse(Horse horse, String imageUrl) {
        this.horse = horse;
        this.imageUrl = imageUrl;
    }

    /** The underlying Horse entity (fields merged into root JSON object). */
    public Horse getHorse() {
        return horse;
    }

    /** Nullable: /api/horses/{id}/photo, or null if the horse has no photo on file. */
    public String getImageUrl() {
        return imageUrl;
    }
}
