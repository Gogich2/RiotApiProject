package org.main.account.security;

import java.util.UUID;

public record AppPrincipal(UUID userId, String email, String displayName) {
}
