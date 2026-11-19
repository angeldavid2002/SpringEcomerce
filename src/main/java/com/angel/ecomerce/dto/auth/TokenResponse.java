package com.angel.ecomerce.dto.auth;

import java.time.Duration;

public record TokenResponse(String token, String type, long expiresIn) {

    public static TokenResponse bearer(String token, Duration expiration) {
        return new TokenResponse(token, "Bearer", expiration.toSeconds());
    }
}
