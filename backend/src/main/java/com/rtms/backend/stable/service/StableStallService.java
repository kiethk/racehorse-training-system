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
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Những gì một Groom được thấy: chỉ các khu có chuồng của mình; chuồng của người khác trong cùng khu vẫn trả về
     * để vẽ khung barn nhưng bị xoá groomId. Trả bản sao, không sửa entity đang được quản lý.
     * (What a groom may see: only the areas holding one of their stalls; other grooms' stalls in those areas are
     * still returned so the barn can be drawn, but without groomId. Copies are returned; managed entities are not
     * modified.)
     */
    public static List<StableStall> scopeForGroom(List<StableStall> all, List<StableStall> ownStalls, Long groomId) {
        // Khu được suy từ chuồng thật của Groom, không từ danh sách đã lọc (ví dụ ?status=AVAILABLE).
        // (Areas come from the groom's actual stalls, not from the possibly filtered list, e.g. ?status=AVAILABLE.)
        Set<Long> ownAreaIds = ownStalls.stream()
                .map(StableStall::getAreaId)
                .collect(Collectors.toSet());
        return all.stream()
                .filter(stall -> ownAreaIds.contains(stall.getAreaId()))
                .map(stall -> groomId.equals(stall.getGroomId()) ? stall : withoutGroom(stall))
                .toList();
    }

    private static StableStall withoutGroom(StableStall stall) {
        StableStall copy = new StableStall();
        copy.setId(stall.getId());
        copy.setAreaId(stall.getAreaId());
        copy.setStallNumber(stall.getStallNumber());
        copy.setStallCode(stall.getStallCode());
        copy.setStatus(stall.getStatus());
        return copy;
    }
}
