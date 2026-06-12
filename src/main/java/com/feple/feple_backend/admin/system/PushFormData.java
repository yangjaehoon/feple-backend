package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.notification.entity.BroadcastNotification;

import java.util.List;

public record PushFormData(
        long deviceCount,
        List<BroadcastNotification> history,
        List<ArtistResponseDto> artists,
        List<FestivalResponseDto> festivals
) {}
