package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.admin.FestivalChecklistRepository;
import com.feple.feple_backend.artist.song.repository.ArtistFestivalSongRepository;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.booth.repository.BoothRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalStatus;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalAttendanceRepository;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.repository.FestivalWeatherRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.service.PostCascadeService;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictFestivalCaches;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.feple.feple_backend.global.PageableFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FestivalServiceImpl implements FestivalService {

    private static final String ERR_END_BEFORE_START = "종료일은 시작일보다 이전일 수 없습니다.";

    private final FestivalRepository festivalRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFestivalSongRepository artistFestivalSongRepository;
    private final NotificationRepository notificationRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final FestivalAttendanceRepository festivalAttendanceRepository;
    private final FileStorageService fileStorageService;
    private final StageRepository stageRepository;
    private final BoothRepository boothRepository;
    private final TimetableRepository timetableRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final PostCascadeService postCascadeService;
    private final FestivalWeatherRepository weatherRepository;
    private final FestivalChecklistRepository festivalChecklistRepository;

    private FestivalResponseDto toDto(Festival festival) {
        return FestivalResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Override
    @Transactional
    @EvictFestivalCaches
    public Long createFestival(FestivalRequestDto dto) {
        if (dto.getEndDate() != null && dto.getStartDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException(ERR_END_BEFORE_START);
        }
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
                .ageRestriction(dto.getAgeRestriction() != null ? dto.getAgeRestriction() : AgeRestriction.ALL_AGES)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
        return festivalRepository.save(festival).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getAllFestivals(List<Genre> genres, List<Region> regions,
                                                     List<AgeRestriction> ageRestrictions,
                                                     boolean includeEnded) {
        LocalDate today = LocalDate.now();
        List<Festival> all = festivalRepository.findByFilters(
            genres == null || genres.isEmpty() ? null : genres,
            regions == null || regions.isEmpty() ? null : regions,
            ageRestrictions == null || ageRestrictions.isEmpty() ? null : ageRestrictions
        );

        List<FestivalStatus> statuses = includeEnded
            ? List.of(FestivalStatus.ACTIVE, FestivalStatus.ENDED)
            : List.of(FestivalStatus.ACTIVE);

        return statuses.stream()
            .flatMap(status -> status.filter(all, today).stream())
            .limit(PageSize.FESTIVALS)
            .map(this::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("allFestivalsForAdmin")
    public List<FestivalResponseDto> getAllFestivalsForAdmin() {
        return getAllFestivals(null, null, null, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getAllActiveFestivalsForAdmin() {
        return getAllFestivalsForAdmin().stream()
                .filter(f -> !f.isEnded())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FestivalDetailResponseDto getFestivalDetail(Long id) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, id, "페스티벌");
        return FestivalDetailResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Override
    @Transactional(readOnly = true)
    public FestivalResponseDto getFestival(Long id) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, id, "페스티벌");
        return toDto(festival);
    }

    @Override
    @Transactional
    @EvictFestivalCaches
    public void updateFestival(Long id, FestivalRequestDto dto) {
        if (dto.getEndDate() != null && dto.getStartDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException(ERR_END_BEFORE_START);
        }
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, id, "페스티벌");

        festival.update(dto.getTitle(), dto.getTitleEn(), dto.getDescription(), dto.getLocation(),
                dto.getStartDate(), dto.getEndDate(),
                dto.getGenres(), dto.getRegion(), dto.getAgeRestriction(),
                dto.getLatitude(), dto.getLongitude());
        String oldPosterKey = festival.getPosterKey();
        festival.updatePoster(dto.getPosterKey());
        if (dto.getPosterKey() != null && !dto.getPosterKey().equals(oldPosterKey)) {
            fileStorageService.deleteFileAfterCommit(oldPosterKey);
        }
    }

    @Override
    @Transactional
    @EvictFestivalCaches
    public void deleteFestival(Long festivalId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        String posterKey = festival.getPosterKey();

        timetableRepository.deleteByFestivalId(festivalId);
        boothRepository.deleteByFestivalId(festivalId);
        stageRepository.deleteByFestivalId(festivalId);
        certificationRepository.deleteByFestivalId(festivalId);
        festivalLikeRepository.deleteByFestivalId(festivalId);
        festivalAttendanceRepository.deleteByFestivalId(festivalId);

        postCascadeService.deletePostsByFestival(festival);

        artistFestivalSongRepository.deleteByFestivalId(festivalId);
        artistFestivalRepository.deleteByFestivalId(festivalId);
        notificationRepository.deleteByFestivalId(festivalId);
        weatherRepository.deleteByFestivalId(festivalId);
        festivalChecklistRepository.deleteByFestivalId(festivalId);
        festivalRepository.deleteById(festivalId);

        fileStorageService.deleteFileAfterCommit(posterKey);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "activeFestivalCount", key = "#today")
    public long countActiveFestivals(LocalDate today) {
        return festivalRepository.countActiveFestivals(today);
    }

    @Override
    public String uploadPosterFile(MultipartFile file, LocalDate startDate) throws IOException {
        return fileStorageService.storeFestivalPoster(file, startDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getLikedFestivals(Long userId) {
        return festivalLikeRepository.findByUserId(userId).stream()
                .map(like -> toDto(like.getFestival()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDto> searchFestivals(String keyword) {
        return festivalRepository.findByTitleKeyword(LikeEscaper.escape(keyword)).stream()
                .limit(10)
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalResponseDto> getFestivalsPage(int page, int size) {
        Page<Festival> result = festivalRepository
                .findAll(PageableFactory.latestStartDate(page, size));
        return result.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalResponseDto> getFestivalsAdminPage(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (keyword == null || keyword.isBlank()) {
            return festivalRepository.findAll(
                    PageableFactory.latestStartDate(page, size))
                    .map(this::toDto);
        }
        return festivalRepository.findByTitleKeywordPaged(LikeEscaper.escape(keyword), pageable).map(this::toDto);
    }

    @Override
    public long getTotalCount() {
        return festivalRepository.count();
    }
}
