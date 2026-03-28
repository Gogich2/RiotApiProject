package org.main.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
        String errorId,
        String code,
        String message,
        String path,
        OffsetDateTime timestamp,
        Map<String, Object> context
) {
}