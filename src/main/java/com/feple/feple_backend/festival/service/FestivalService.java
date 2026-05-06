package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.booth.repository.BoothRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalStatus;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final FileStorageService fileStorageService;
    private final StageRepository stageRepository;
    private final BoothRepository boothRepository;
    private final TimetableRepository timetableRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    private FestivalResponseDto toDto(Festival festival) {
        return FestivalResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Transactional
    public Long createFestival(FestivalRequestDto dto) {
        Festival festival = Festival.builder()
                .title(dto.getTitle())
                .titleEn(dto.getTitleEn())
                .description(dto.getDescription())
                .location(dto.getLocation())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .posterKey(dto.getPosterKey())
                .genres(dto.getGenres() != null ? dto.getGenres() : new java.util.ArrayList<>())
                .region(dto.getRegion())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();

        Festival saved = festivalRepository.save(festival);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getAllFestivals(List<Genre> genres, List<Region> regions,
                                                     boolean includeEnded) {
        LocalDate today = LocalDate.now();
        List<Festival> all = festivalRepository.findByFilters(
            genres == null || genres.isEmpty() ? null : genres,
            regions == null || regions.isEmpty() ? null : regions
        );

        List<FestivalStatus> statuses = includeEnded
            ? List.of(FestivalStatus.ACTIVE, FestivalStatus.ENDED)  // ACTIVE 먼저, ENDED 뒤에
            : List.of(FestivalStatus.ACTIVE);

        return statuses.stream()
            .flatMap(status -> status.filter(all, today).stream())
            .limit(200)
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public FestivalDetailResponseDto getFestivalDetail(Long id) {
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 페스티벌입니다. id=" + id));

        return FestivalDetailResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Transactional(readOnly = true)
    public FestivalResponseDto getFestival(Long id) {
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 페스티벌입니다. id=" + id));

        return toDto(festival);
    }

    @Transactional
    public void updateFestival(Long id, FestivalRequestDto dto) {
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 페스티벌입니다. id=" + id));

        festival.setTitle(dto.getTitle());
        festival.setTitleEn(dto.getTitleEn());
        festival.setDescription(dto.getDescription());
        festival.setLocation(dto.getLocation());
        festival.setStartDate(dto.getStartDate());
        festival.setEndDate(dto.getEndDate());
        if (dto.getPosterKey() != null && !dto.getPosterKey().equals(festival.getPosterKey())) {
            String oldKey = festival.getPosterKey();
            festival.setPosterKey(dto.getPosterKey());
            fileStorageService.deleteFile(oldKey);
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
    public void deleteFestival(Long festivalId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 페스티벌입니다. id=" + festivalId));
        String posterKey = festival.getPosterKey();

        // 타임테이블, 부스, 스테이지 삭제
        timetableRepository.deleteByFestivalId(festivalId);
        boothRepository.deleteByFestivalId(festivalId);
        stageRepository.deleteByFestivalId(festivalId);

        // 인증 및 좋아요 삭제
        certificationRepository.deleteByFestivalId(festivalId);
        festivalLikeRepository.deleteByFestivalId(festivalId);

        // 게시글 삭제 (PostLike 먼저, Comment는 Post cascade)
        List<Post> festivalPosts = postRepository.findByFestival(festival);
        for (Post post : festivalPosts) {
            postLikeRepository.deleteByPostId(post.getId());
        }
        postRepository.deleteAll(festivalPosts);

        // 아티스트-페스티벌 연결 및 페스티벌 삭제
        artistFestivalRepository.deleteByFestivalId(festivalId);
        festivalRepository.deleteById(festivalId);

        // DB 삭제 완료 후 S3 포스터 삭제
        fileStorageService.deleteFile(posterKey);
    }

    @Transactional
    public Page<FestivalResponseDto> getFestivalsPage(int page, int size) {
        Page<Festival> result = festivalRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate")));
        return result.map(this::toDto);
    }

}
