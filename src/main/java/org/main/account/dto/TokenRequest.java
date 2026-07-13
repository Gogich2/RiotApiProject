package org.main.account.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) {
}
