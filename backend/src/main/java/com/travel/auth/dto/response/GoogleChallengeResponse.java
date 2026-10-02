package com.travel.auth.dto.response;
import java.util.UUID;
public record GoogleChallengeResponse(UUID challengeId, String nonce, String webClientId, long expiresIn) { }
