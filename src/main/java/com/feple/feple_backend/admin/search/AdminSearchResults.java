package com.feple.feple_backend.admin.search;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.user.dto.UserResponseDto;

public record AdminSearchResults(
        AdminSearchSection<UserResponseDto> users,
        AdminSearchSection<PostResponseDto> posts,
        AdminSearchSection<ArtistResponseDto> artists,
        AdminSearchSection<FestivalResponseDto> festivals
) {
    public boolean isEmpty() {
        return users.isEmpty() && posts.isEmpty() && artists.isEmpty() && festivals.isEmpty();
    }
}
