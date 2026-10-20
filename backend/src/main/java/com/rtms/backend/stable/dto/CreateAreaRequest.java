package com.rtms.backend.stable.dto;

import com.rtms.backend.stable.enums.AreaType;
import jakarta.validation.constraints.NotNull;

public class CreateAreaRequest {

    @NotNull(message = "Area type is required")
    private AreaType type;

    public CreateAreaRequest() {
    }

    public CreateAreaRequest(AreaType type) {
        this.type = type;
    }

    public AreaType getType() {
        return type;
    }

    public void setType(AreaType type) {
        this.type = type;
    }
}
