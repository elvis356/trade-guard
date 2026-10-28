package com.tradeguard.auth;

import com.tradeguard.user.dto.UserResponse;

import java.time.Instant;

public record LoginResponse(UserResponse user, Instant expiresAt) {
}
