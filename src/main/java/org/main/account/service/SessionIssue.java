package org.main.account.service;

import java.time.OffsetDateTime;

public record SessionIssue(String rawToken, OffsetDateTime expiresAt) {
}
