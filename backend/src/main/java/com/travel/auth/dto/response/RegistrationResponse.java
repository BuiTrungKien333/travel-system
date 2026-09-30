package com.travel.auth.dto.response;

public record RegistrationResponse(boolean verificationRequired, long resendAfterSeconds) { }
