package com.example.thinkmap.dto.auth;

import com.example.thinkmap.domain.entity.User;

public record UserInfoResponse(
        Long id,
        String name,
        String email,
        String profileImageUrl,
        String provider
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getProvider().name().toLowerCase()
        );
    }
}
