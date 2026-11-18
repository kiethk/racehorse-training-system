package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SearchClinicalKnowledgeTool implements AiToolHandler {

    private final ClinicalRetrievalService clinicalRetrievalService;
    private final AiToolSupport support;

    @Autowired
    public SearchClinicalKnowledgeTool(ClinicalRetrievalService clinicalRetrievalService, AiToolSupport support) {
        this.clinicalRetrievalService = clinicalRetrievalService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "searchClinicalKnowledge";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        String query = (String) arguments.get("query");
        Horse horse = support.resolveHorse(arguments);
        Long horseId = horse != null ? horse.getId() : support.parseLong(arguments.get("horseId"));
        Integer limit = arguments.get("limit") != null ? ((Number) arguments.get("limit")).intValue() : 5;

        List<ClinicalChunkResponseDto> chunks = clinicalRetrievalService.retrieveSimilarClinicalRecords(
                query, horseId, currentUser.getRole(), 0.60, limit);
        return chunks;
    }
}
