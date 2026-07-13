package org.main.account.dto;

import java.util.UUID;
import org.main.account.entity.AppUserEntity;
import org.main.account.security.AppPrincipal;

public record AuthUserResponse(UUID id, String email, String displayName) {

    public static AuthUserResponse from(AppUserEntity user) {
        return new AuthUserResponse(user.getId(), user.getEmailNormalized(), user.getDisplayName());
    }

    public static AuthUserResponse from(AppPrincipal principal) {
        return new AuthUserResponse(principal.userId(), principal.email(), principal.displayName());
    }
}
