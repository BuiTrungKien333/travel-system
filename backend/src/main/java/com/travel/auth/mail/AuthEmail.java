package com.travel.auth.mail;

import com.travel.auth.entity.ActionPurpose;

import java.time.Instant;

/**
 * Raw one-use token exists only in memory; never log this event or recipient.
 */
public record AuthEmail(String recipient, String token, ActionPurpose purpose, String locale, Instant expiresAt) {

    @Override
    public String toString() {
        return "AuthEmail[purpose=" + purpose + "]";
    }
}
