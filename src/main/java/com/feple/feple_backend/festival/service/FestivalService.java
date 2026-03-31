package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.FileStorageService;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private FestivalResponseDto toDto(Festival festival) {
        return FestivalResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Transactional
    public Long createFestival(FestivalRequestDto dto) {
        Festival festival = Festival.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .location(dto.getLocation())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .posterKey(dto.getPosterUrl())
                .genres(dto.getGenres() != null ? dto.getGenres() : new java.util.ArrayList<>())
                .region(dto.getRegion())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();

        @SuppressWarnings("null")
        Festival saved = festivalRepository.save(festival);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getAllFestivals(List<Genre> genres, List<Region> regions) {
        return festivalRepository.findAllByOrderByStartDateDesc()
                .stream()
                .filter(f -> genres == null || genres.isEmpty() || !Collections.disjoint(f.getGenres(), genres))
                .filter(f -> regions == null || regions.isEmpty() || regions.contains(f.getRegion()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public FestivalDetailResponseDto getFestivalDetail(Long id) {
        @SuppressWarnings("null")
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + id));

        return FestivalDetailResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Transactional(readOnly = true)
    public FestivalResponseDto getFestival(Long id) {
        @SuppressWarnings("null")
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + id));

        return toDto(festival);
    }

    @Transactional
    public void updateFestival(Long id, FestivalRequestDto dto) {
        @SuppressWarnings("null")
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + id));

        festival.setTitle(dto.getTitle());
        festival.setDescription(dto.getDescription());
        festival.setLocation(dto.getLocation());
        festival.setStartDate(dto.getStartDate());
        festival.setEndDate(dto.getEndDate());
        if (dto.getPosterUrl() != null) {
            festival.setPosterKey(dto.getPosterUrl());
        }
        if (dto.getGenres() != null) {
            festival.setGenres(dto.getGenres());
        }
        if (dto.getRegion() != null) {
            festival.setRegion(dto.getRegion());
        }
        if (dto.getLatitude() != null) {
            festival.setLatitude(dto.getLatitude());
        }
        if (dto.getLongitude() != null) {
            festival.setLongitude(dto.getLongitude());
        }
    }

    @Transactional
    public boolean toggleLike(Long festivalId, Long userId) {
        @SuppressWarnings("null")
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다."));
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return festivalLikeRepository.findByUserIdAndFestivalId(userId, festivalId)
                .map(like -> {
                    festivalLikeRepository.delete(like);
                    festival.decrementLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    festivalLikeRepository.save(FestivalLike.of(user, festival));
                    festival.incrementLikeCount();
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long festivalId, Long userId) {
        return festivalLikeRepository.existsByUserIdAndFestivalId(userId, festivalId);
    }

    @Transactional
    public void deleteFestival(Long festivalId) {

        if (!festivalRepository.existsById(festivalId)) {
            throw new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + festivalId);
        }

        artistFestivalRepository.deleteByFestivalId(festivalId);
        festivalRepository.deleteById(festivalId);
    }

    @Transactional
    public Page<FestivalResponseDto> getFestivalsPage(int page, int size) {
        Page<Festival> result = festivalRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate")));
        return result.map(this::toDto);
    }

}
