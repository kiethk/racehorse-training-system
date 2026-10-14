package com.rtms.backend.horse.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;

/**
 * API response DTO for horse endpoints.
 * Includes all Horse fields (unwrapped) plus an optional imageUrl and optional specialty.
 * imageUrl points to GET /api/horses/{id}/photo — a protected endpoint
 * that requires HORSE_VIEW and streams the file; null when no photo exists.
 * specialty is included when includeSpecialty=true is requested on horse list/detail.
 */
public class HorseResponse {

    @JsonUnwrapped
    private final Horse horse;

    private final String imageUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final HorseSpecialtyResponse specialty;

    public HorseResponse(Horse horse, String imageUrl) {
        this(horse, imageUrl, null);
    }

    public HorseResponse(Horse horse, String imageUrl, HorseSpecialtyResponse specialty) {
        this.horse = horse;
        this.imageUrl = imageUrl;
        this.specialty = specialty;
    }

    /** The underlying Horse entity (fields merged into root JSON object). */
    public Horse getHorse() {
        return horse;
    }

    /** Nullable: /api/horses/{id}/photo, or null if the horse has no photo on file. */
    public String getImageUrl() {
        return imageUrl;
    }

    /** Nullable: populated when includeSpecialty=true is queried. */
    public HorseSpecialtyResponse getSpecialty() {
        return specialty;
    }
}
