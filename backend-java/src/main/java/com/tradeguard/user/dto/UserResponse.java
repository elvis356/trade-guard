package com.tradeguard.user.dto;

import com.tradeguard.user.AppUser;
import com.tradeguard.user.Role;

import java.time.Instant;

public record UserResponse(Long id, String username, Role role, boolean enabled, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(),
                user.isEnabled(), user.getCreatedAt());
    }
}
