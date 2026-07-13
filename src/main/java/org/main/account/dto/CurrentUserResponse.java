package org.main.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record CurrentUserResponse(
        boolean authenticated,
        @JsonInclude(JsonInclude.Include.NON_NULL) AuthUserResponse user
) {

    public static CurrentUserResponse anonymous() {
        return new CurrentUserResponse(false, null);
    }

    public static CurrentUserResponse authenticated(AuthUserResponse user) {
        return new CurrentUserResponse(true, user);
    }
}
