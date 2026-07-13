package org.main.dto.frontend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiotIdResolveRequest(
        @NotBlank @Size(max = 60) String gameName,
        @NotBlank @Size(max = 20) String tagLine
) {
}
