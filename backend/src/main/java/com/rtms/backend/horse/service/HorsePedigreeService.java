package com.rtms.backend.horse.service;

import com.rtms.backend.horse.dto.CreateHorsePedigreeRequest;
import com.rtms.backend.horse.dto.HorsePedigreeResponse;
import com.rtms.backend.horse.dto.HorseSummaryResponse;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.horse.util.UelnUtils;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class HorsePedigreeService {
    private final HorsePedigreeRepository horsePedigreeRepository;
    private final HorseRepository horseRepository;
    private final PedigreeBirthDateValidator birthDates;

    public HorsePedigreeService(HorsePedigreeRepository horsePedigreeRepository, HorseRepository horseRepository,
                               PedigreeBirthDateValidator birthDates) {
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.horseRepository = horseRepository;
        this.birthDates = birthDates;
    }

    public HorsePedigreeResponse getByHorseId(Long horseId) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));

        HorsePedigree pedigree = horsePedigreeRepository.findByHorseId(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedigree not found"));

        Horse sire = null;
        Horse dam = null;

        if (pedigree.getSireId() != null) {
            sire = horseRepository.findById(pedigree.getSireId()).orElse(null);
        }

        if (pedigree.getDamId() != null) {
            dam = horseRepository.findById(pedigree.getDamId()).orElse(null);
        }

        return toResponse(pedigree, horse, sire, dam);
    }

    @Transactional
    public HorsePedigreeResponse createOrUpdate(Long horseId, CreateHorsePedigreeRequest request) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horse not found"));

        // 1. Normalize and validate child UELN
        String childUeln = UelnUtils.cleanAndValidate(request.getRegistrationNumber(), "Horse registration number (UELN)");
        if (childUeln != null) {
            List<Horse> existing = horseRepository.findByRegistrationNumber(childUeln);
            for (Horse other : existing) {
                if (!other.getId().equals(horseId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Another horse with this UELN already exists");
                }
            }
        }

        // 2. Resolve Sire
        Long sireId = request.getSireId();
        String sireName = request.getSireName() != null && !request.getSireName().isBlank() ? request.getSireName().trim() : null;
        String sireUeln = UelnUtils.cleanAndValidate(request.getSireRegistrationNumber(), "Sire UELN");
        Horse sire = null;

        if (sireId != null) {
            if (Objects.equals(horseId, sireId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse cannot be its own sire");
            }
            sire = horseRepository.findById(sireId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sire horse not found"));
            if (sireName == null) {
                sireName = sire.getName();
            }
            if (sireUeln == null) {
                sireUeln = sire.getRegistrationNumber();
            }
        } else if (sireUeln != null) {
            // Check if sire UELN matches an internal horse
            List<Horse> matches = horseRepository.findByRegistrationNumber(sireUeln);
            if (!matches.isEmpty()) {
                Horse matched = matches.get(0);
                if (Objects.equals(horseId, matched.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse cannot be its own sire");
                }
                sireId = matched.getId();
                sire = matched;
                if (sireName == null) {
                    sireName = matched.getName();
                }
            }
        }

        // 3. Resolve Dam
        Long damId = request.getDamId();
        String damName = request.getDamName() != null && !request.getDamName().isBlank() ? request.getDamName().trim() : null;
        String damUeln = UelnUtils.cleanAndValidate(request.getDamRegistrationNumber(), "Dam UELN");
        Horse dam = null;

        if (damId != null) {
            if (Objects.equals(horseId, damId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse cannot be its own dam");
            }
            dam = horseRepository.findById(damId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dam horse not found"));
            if (damName == null) {
                damName = dam.getName();
            }
            if (damUeln == null) {
                damUeln = dam.getRegistrationNumber();
            }
        } else if (damUeln != null) {
            // Check if dam UELN matches an internal horse
            List<Horse> matches = horseRepository.findByRegistrationNumber(damUeln);
            if (!matches.isEmpty()) {
                Horse matched = matches.get(0);
                if (Objects.equals(horseId, matched.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horse cannot be its own dam");
                }
                damId = matched.getId();
                dam = matched;
                if (damName == null) {
                    damName = matched.getName();
                }
            }
        }

        // 4. Validate parents distinctness
        if (sireId != null && damId != null && Objects.equals(sireId, damId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sire and dam cannot be the same horse");
        }

        // 5. Cross-validate pedigree UELNs
        UelnUtils.validatePedigreeUelns(childUeln, sireUeln, damUeln);
        birthDates.validate(horse.getDateOfBirth(), sire, dam);
        validateParentSexes(sire, dam);

        // 6. Save updates
        HorsePedigree pedigree = horsePedigreeRepository.findByHorseId(horseId).orElseGet(HorsePedigree::new);
        pedigree.setHorseId(horseId);
        pedigree.setSireId(sireId);
        pedigree.setSireName(sireName);
        pedigree.setSireRegistrationNumber(sireUeln);
        pedigree.setDamId(damId);
        pedigree.setDamName(damName);
        pedigree.setDamRegistrationNumber(damUeln);
        pedigree.setPedigreeNotes(request.getPedigreeNotes());

        horse.setRegistrationNumber(childUeln);
        horse.setRegistryName(request.getRegistryName());
        horse = horseRepository.save(horse);

        HorsePedigree saved = horsePedigreeRepository.save(pedigree);

        return toResponse(saved, horse, sire, dam);
    }

    /**
     * A sire must be male and a dam female. Only parents registered here with a known sex
     * can be checked; an external UELN or an unknown sex is accepted as before.
     */
    public void validateParentSexes(Horse sire, Horse dam) {
        if (sire != null && sire.getSex() == HorseSex.FEMALE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The sire must be a male horse");
        }
        if (dam != null && dam.getSex() == HorseSex.MALE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The dam must be a female horse");
        }
    }

    /** {@link #validateParentSexes(Horse, Horse)} for parents given by (already normalized) UELN. */
    public void validateParentSexesByUeln(String sireUeln, String damUeln) {
        validateParentSexes(findByUeln(sireUeln), findByUeln(damUeln));
    }

    private Horse findByUeln(String ueln) {
        if (ueln == null) return null;
        List<Horse> matches = horseRepository.findByRegistrationNumber(ueln);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private HorsePedigreeResponse toResponse(HorsePedigree pedigree, Horse horse, Horse sire, Horse dam) {
        String sireName = sire != null ? sire.getName() : pedigree.getSireName();
        String sireUeln = sire != null && sire.getRegistrationNumber() != null ? sire.getRegistrationNumber() : pedigree.getSireRegistrationNumber();

        String damName = dam != null ? dam.getName() : pedigree.getDamName();
        String damUeln = dam != null && dam.getRegistrationNumber() != null ? dam.getRegistrationNumber() : pedigree.getDamRegistrationNumber();

        return new HorsePedigreeResponse(
                pedigree.getId(),
                toSummary(horse),
                toSummary(sire),
                toSummary(dam),
                horse.getRegistrationNumber(),
                horse.getRegistryName(),
                pedigree.getPedigreeNotes(),
                sireName,
                sireUeln,
                damName,
                damUeln);
    }

    private HorseSummaryResponse toSummary(Horse horse) {
        if (horse == null) {
            return null;
        }

        return new HorseSummaryResponse(
                horse.getId(),
                horse.getName(),
                horse.getBreed(),
                horse.getDateOfBirth(),
                horse.getCurrentStatus() != null ? horse.getCurrentStatus().name() : null);
    }
}
