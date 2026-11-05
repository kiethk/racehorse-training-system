package com.rtms.backend.stable.dto;

import java.util.List;

public record AreaTrainerChangeCheck(Long areaId, Long trainerId, boolean canChange, List<String> reasons) {}
