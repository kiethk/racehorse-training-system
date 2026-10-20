package com.rtms.backend.stable.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BlockGroomAssignmentDto {

    @NotNull(message = "areaId must not be null")
    private Long areaId;

    /** 1-based block number: 1 (stalls 1-3), 2 (stalls 4-6), 3 (stalls 7-9), 4 (stalls 10-12) */
    private Integer blockNumber;

    /** 0-based block index alternative: 0..3 */
    private Integer blockIndex;

    /** null means unassign groom */
    private Long groomId;

    /** Expected groom ID at the time data was loaded (for conflict detection) */
    private Long expectedGroomId;

    /** Baseline in stall-number order; supports legacy partial/mixed blocks. Null entries are valid. */
    private List<Long> expectedGroomIds;

    private boolean expectedGroomIdSet = false;

    public BlockGroomAssignmentDto() {
    }

    public BlockGroomAssignmentDto(Long areaId, Integer blockNumber, Long groomId) {
        this.areaId = areaId;
        this.blockNumber = blockNumber;
        this.groomId = groomId;
    }

    public BlockGroomAssignmentDto(Long areaId, Integer blockNumber, Long groomId, Long expectedGroomId) {
        this.areaId = areaId;
        this.blockNumber = blockNumber;
        this.groomId = groomId;
        this.expectedGroomId = expectedGroomId;
        this.expectedGroomIdSet = true;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Integer getBlockNumber() {
        if (blockNumber != null) {
            return blockNumber;
        }
        if (blockIndex != null) {
            return blockIndex + 1;
        }
        return null;
    }

    public void setBlockNumber(Integer blockNumber) {
        this.blockNumber = blockNumber;
    }

    public Integer getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(Integer blockIndex) {
        this.blockIndex = blockIndex;
    }

    public Long getGroomId() {
        return groomId;
    }

    public void setGroomId(Long groomId) {
        this.groomId = groomId;
    }

    public Long getExpectedGroomId() {
        return expectedGroomId;
    }

    public void setExpectedGroomId(Long expectedGroomId) {
        this.expectedGroomId = expectedGroomId;
        this.expectedGroomIdSet = true;
    }

    public List<Long> getExpectedGroomIds() {
        return expectedGroomIds;
    }

    public void setExpectedGroomIds(List<Long> expectedGroomIds) {
        this.expectedGroomIds = expectedGroomIds;
    }

    @JsonIgnore
    public boolean isExpectedGroomIdSet() {
        return expectedGroomIdSet;
    }
}
