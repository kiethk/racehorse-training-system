package com.rtms.backend.horse.dto;

/**
 * The mutable horse facts an owner may ask to change. The client sends the full desired
 * state (pre-filled with current values); the server works out what actually changed.
 * UELN, sex, date of birth and registry name are deliberately absent: they cannot change.
 */
public record HorseEditFields(
        String name,
        String breed,
        String sireName,
        String sireRegistrationNumber,
        String damName,
        String damRegistrationNumber) {
}
