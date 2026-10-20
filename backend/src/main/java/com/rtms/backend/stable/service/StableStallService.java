package com.rtms.backend.stable.service;

import com.rtms.backend.stable.dto.BlockGroomAssignmentDto;
import com.rtms.backend.stable.dto.BatchStableAssignmentRequest;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class StableStallService {
    private final StableStallRepository stalls;
    private final StableAssignmentService assignments;

    public StableStallService(StableStallRepository stalls, StableAssignmentService assignments) {
        this.stalls = stalls;
        this.assignments = assignments;
    }

    /** Legacy stall address, but an explicit baseline and whole-block policy are always required. */
    @Transactional
    public StableStall assignGroom(Long stallId, BlockGroomAssignmentDto request) {
        StableStall stall = stalls.findById(stallId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Stall not found"));
        request.setAreaId(stall.getAreaId());
        request.setBlockNumber((stall.getStallNumber() - 1) / StableAssignmentService.STALLS_PER_BLOCK + 1);
        assignments.applyAssignments(new BatchStableAssignmentRequest(null, List.of(request)));
        return stalls.findById(stallId).orElseThrow();
    }
}
