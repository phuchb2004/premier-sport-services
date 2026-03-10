package com.premiersport.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
