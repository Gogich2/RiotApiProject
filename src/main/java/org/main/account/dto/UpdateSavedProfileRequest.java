package org.main.account.dto;

import jakarta.validation.constraints.Size;

public record UpdateSavedProfileRequest(
        @Size(max = 80) String personalLabel,
        boolean isDefault
) {
}
