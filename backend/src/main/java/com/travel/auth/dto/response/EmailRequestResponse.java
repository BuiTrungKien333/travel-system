package com.travel.auth.dto.response;

/** Same payload for missing, disabled and eligible accounts. Never return the action token. */
public record EmailRequestResponse(long resendAfterSeconds) { }
