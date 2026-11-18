package com.rtms.backend.ai.tool.handler.owner;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetMyHorsePedigreeInfoTool implements AiToolHandler {

    private final HorsePedigreeRepository horsePedigreeRepository;
    private final AiToolSupport support;

    @Autowired
    public GetMyHorsePedigreeInfoTool(@Autowired(required = false) HorsePedigreeRepository horsePedigreeRepository,
                                      AiToolSupport support) {
        this.horsePedigreeRepository = horsePedigreeRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getMyHorsePedigreeInfo";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        support.verifyHorseOwnershipIfOwner(horse.getId(), currentUser);

        Optional<HorsePedigree> pedigreeOpt = (horsePedigreeRepository != null)
                ? horsePedigreeRepository.findByHorseId(horse.getId())
                : Optional.empty();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse.getName());
        result.put("breed", horse.getBreed() != null ? horse.getBreed() : "N/A");
        result.put("sex", horse.getSex() != null ? horse.getSex().name() : "N/A");
        result.put("dateOfBirth", horse.getDateOfBirth() != null ? horse.getDateOfBirth().toString() : "N/A");
        result.put("sire", pedigreeOpt.map(HorsePedigree::getSireName).filter(s -> !s.isBlank()).orElse("Unknown"));
        result.put("sireRegistrationNumber", pedigreeOpt.map(HorsePedigree::getSireRegistrationNumber).filter(s -> !s.isBlank()).orElse("N/A"));
        result.put("dam", pedigreeOpt.map(HorsePedigree::getDamName).filter(d -> !d.isBlank()).orElse("Unknown"));
        result.put("damRegistrationNumber", pedigreeOpt.map(HorsePedigree::getDamRegistrationNumber).filter(d -> !d.isBlank()).orElse("N/A"));
        result.put("origin", pedigreeOpt.map(HorsePedigree::getPedigreeNotes).filter(n -> !n.isBlank()).orElse("Vietnam"));
        result.put("pedigreeNotes", pedigreeOpt.map(HorsePedigree::getPedigreeNotes).filter(n -> !n.isBlank()).orElse(""));
        result.put("pedigreeGeneration", pedigreeOpt.map(p -> "Sire: " + (p.getSireName() != null ? p.getSireName() : "Unknown") + ", Dam: " + (p.getDamName() != null ? p.getDamName() : "Unknown")).orElse("No detailed pedigree record available"));
        result.put("actionUrl", horse.getId() != null ? "/owner/horses/" + horse.getId() : "/owner/horses");
        return result;
    }
}
