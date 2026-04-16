package com.feple.feple_backend.search.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.search.dto.SearchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final PostRepository postRepository;
    private final FileStorageService fileStorageService;

    private static final int MAX_RESULTS = 10;

    @Transactional(readOnly = true)
    public SearchResultDto search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new SearchResultDto(List.of(), List.of(), List.of());
        }
        String kw = keyword.trim();

        List<ArtistResponseDto> artists = artistRepository
                .findByNameContainingIgnoreCaseOrderByNameAsc(kw)
                .stream()
                .limit(MAX_RESULTS)
                .map(a -> ArtistResponseDto.from(a, fileStorageService.buildUrl(a.getProfileImageKey())))
                .toList();

        List<FestivalResponseDto> festivals = festivalRepository
                .findByTitleKeyword(kw)
                .stream()
                .limit(MAX_RESULTS)
                .map(f -> FestivalResponseDto.from(f, fileStorageService.buildUrl(f.getPosterKey())))
                .toList();

        List<PostResponseDto> posts = postRepository
                .findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(kw, PageRequest.of(0, MAX_RESULTS))
                .stream()
                .map(PostResponseDto::from)
                .toList();

        return new SearchResultDto(artists, festivals, posts);
    }
}
