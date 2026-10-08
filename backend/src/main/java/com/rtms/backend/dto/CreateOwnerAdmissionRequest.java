package com.rtms.backend.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Only owner-supplied snapshot fields; no Horse, pedigree, stall or status IDs. */
public record CreateOwnerAdmissionRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String breed,
        @PastOrPresent LocalDate dateOfBirth,
        @Pattern(regexp = "^\\s*$|^[a-zA-Z0-9]{15}$", message = "UELN must be 15 alphanumeric characters")
        String registrationNumber,
        @Size(max = 255) String registryName,
        @Size(max = 255) String sireName,
        @Pattern(regexp = "^\\s*$|^[a-zA-Z0-9]{15}$", message = "Sire UELN must be 15 alphanumeric characters")
        String sireRegistrationNumber,
        @Size(max = 255) String damName,
        @Pattern(regexp = "^\\s*$|^[a-zA-Z0-9]{15}$", message = "Dam UELN must be 15 alphanumeric characters")
        String damRegistrationNumber,
        @Size(max = 2000) String pedigreeNotes) {

    // Do not silently ignore horseId, sireId, damId, stallId, horseStatus or any unexpected input.
    @JsonAnySetter
    public void rejectUnexpectedField(String name, Object value) {
        throw new IllegalArgumentException("Unsupported admission field: " + name);
    }
}
