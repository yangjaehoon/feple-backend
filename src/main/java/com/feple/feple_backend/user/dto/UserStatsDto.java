package com.feple.feple_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserStatsDto {
    private long postCount;
    private long commentCount;
}
