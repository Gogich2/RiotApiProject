package org.main.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 60) String displayName
) {
}
