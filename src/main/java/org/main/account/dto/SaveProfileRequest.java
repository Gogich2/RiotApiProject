package org.main.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveProfileRequest(
        @NotBlank @Size(max = 128) String puuid,
        @Size(max = 80) String personalLabel
) {
}
