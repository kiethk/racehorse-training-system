package com.rtms.backend.horse.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.horse.util.UelnUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Component
public class PedigreeBirthDateValidator {
    private final HorseRepository horses;

    public PedigreeBirthDateValidator(HorseRepository horses) {
        this.horses = horses;
    }

    public void validateByUeln(LocalDate childBirthDate, String sireUeln, String damUeln) {
        if (childBirthDate == null) return;
        validateParent(childBirthDate, findParent(sireUeln), "sire");
        validateParent(childBirthDate, findParent(damUeln), "dam");
    }

    public void validate(LocalDate childBirthDate, Horse sire, Horse dam) {
        validateParent(childBirthDate, sire, "sire");
        validateParent(childBirthDate, dam, "dam");
    }

    private Horse findParent(String rawUeln) {
        String ueln = UelnUtils.normalize(rawUeln);
        return ueln == null ? null : horses.findByRegistrationNumber(ueln).stream().findFirst().orElse(null);
    }

    private void validateParent(LocalDate childBirthDate, Horse parent, String relation) {
        if (childBirthDate == null || parent == null || parent.getDateOfBirth() == null) return;
        if (!childBirthDate.isAfter(parent.getDateOfBirth())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Horse date of birth must be after its " + relation + "'s date of birth.");
        }
    }
}
