package com.rtms.backend.stable.dto;

import java.util.List;

public record AreaDeletionCheck(Long areaId, String areaCode, int stallCount,
                                boolean canDelete, List<String> reasons) { }
