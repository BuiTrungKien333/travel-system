package com.travel.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleLoginResponse(Status status, AuthResponse auth, String email, Long resendAfterSeconds) {
    public enum Status {
        AUTHENTICATED,
        VERIFICATION_REQUIRED,
    }

    public static GoogleLoginResponse authenticated(AuthResponse auth) {
        return new GoogleLoginResponse(Status.AUTHENTICATED, auth, null, null);
    }

    public static GoogleLoginResponse verification(String email, long cooldown) {
        return new GoogleLoginResponse(Status.VERIFICATION_REQUIRED, null, email, cooldown);
    }
}
