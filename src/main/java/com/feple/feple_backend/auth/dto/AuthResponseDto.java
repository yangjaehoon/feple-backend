package com.feple.feple_backend.auth.dto;
import com.feple.feple_backend.user.dto.UserResponseDto;

public record AuthResponseDto(UserResponseDto user, String accessToken) {}