package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalStatus;
import com.feple.feple_backend.festival.entity.FestivalUpdateFields;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.FullTextSearchValidator;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.MusicGenre;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.PageableFactory;
import com.feple.feple_backend.global.cache.EvictFestivalCaches;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FestivalServiceImpl implements FestivalService, FestivalAdminService {

    private static final String ERR_END_BEFORE_START = "종료일은 시작일보다 이전일 수 없습니다.";

    private final FestivalRepository festivalRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final FileStorageService fileStorageService;

    private FestivalResponseDto toDto(Festival festival) {
        return FestivalResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Override
    @Transactional
    @EvictFestivalCaches
    public Long createFestival(FestivalRequestDto dto) {
        validateDateRange(dto);
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
    public List<FestivalResponseDto> getAllFestivals(FestivalFilterCriteria criteria) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        // includeEnded=false이면 DB에서 종료된 축제를 미리 제외해 메모리 로드 최소화
        LocalDate activeFrom = criteria.includeEnded() ? null : today;
        List<MusicGenre> genres = criteria.genres();
        List<Region> regions = criteria.regions();
        List<AgeRestriction> ageRestrictions = criteria.ageRestrictions();
        List<Festival> all = festivalRepository.findByFilters(
            genres == null || genres.isEmpty() ? null : genres,
            regions == null || regions.isEmpty() ? null : regions,
            ageRestrictions == null || ageRestrictions.isEmpty() ? null : ageRestrictions,
            activeFrom,
            PageRequest.of(0, PageSize.FESTIVAL_FILTER_FETCH_CAP)
        );

        List<FestivalStatus> statuses = criteria.includeEnded()
            ? List.of(FestivalStatus.ACTIVE, FestivalStatus.ENDED)
            : List.of(FestivalStatus.ACTIVE);

        Stream<Festival> stream = statuses.stream()
            .flatMap(status -> status.filter(all, today).stream());

        String sort = criteria.sort();
        if ("date_asc".equals(sort)) {
            stream = stream.sorted(Comparator.comparing(Festival::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("date_desc".equals(sort)) {
            stream = stream.sorted(Comparator.comparing(Festival::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return stream
            .limit(PageSize.FESTIVALS)
            .map(this::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalResponseDto> getFestivalsPage(FestivalFilterCriteria criteria, Pageable pageable) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate activeFrom = criteria.includeEnded() ? null : today;
        List<MusicGenre> genres = criteria.genres();
        List<Region> regions = criteria.regions();
        List<AgeRestriction> ageRestrictions = criteria.ageRestrictions();

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(criteria.sort()));
        Page<Festival> page = festivalRepository.findByFiltersPage(
            genres == null || genres.isEmpty() ? null : genres,
            regions == null || regions.isEmpty() ? null : regions,
            ageRestrictions == null || ageRestrictions.isEmpty() ? null : ageRestrictions,
            activeFrom,
            sorted
        );
        return page.map(this::toDto);
    }

    // 명시적 정렬이 없으면 다가오는 순(오름차순)을 기본값으로 — 브라우즈 화면에서 가장 자연스러운 순서
    private Sort resolveSort(String sort) {
        if ("date_desc".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "startDate");
        }
        return Sort.by(Sort.Direction.ASC, "startDate");
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("allFestivalsForAdmin")
    public List<FestivalResponseDto> getAllFestivalsForAdmin() {
        return getAllFestivals(FestivalFilterCriteria.forAdmin());
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
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findByIdAndDeletedAtIsNull, id, "페스티벌");
        return FestivalDetailResponseDto.from(festival, fileStorageService.buildUrl(festival.getPosterKey()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "festivalDetail", key = "#festivalId")
    public FestivalResponseDto getFestival(Long festivalId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findByIdAndDeletedAtIsNull, festivalId, "페스티벌");
        return toDto(festival);
    }

    @Override
    @Transactional
    @EvictFestivalCaches
    @CacheEvict(value = "festivalDetail", key = "#festivalId")
    public void updateFestival(Long festivalId, FestivalRequestDto dto) {
        validateDateRange(dto);
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");

        festival.update(new FestivalUpdateFields(
                dto.getTitle(), dto.getTitleEn(), dto.getDescription(), dto.getLocation(),
                dto.getStartDate(), dto.getEndDate(),
                dto.getGenres(), dto.getRegion(), dto.getAgeRestriction(),
                dto.getLatitude(), dto.getLongitude()));
        String oldPosterKey = festival.getPosterKey();
        festival.updatePoster(dto.getPosterKey());
        if (dto.getPosterKey() != null && !dto.getPosterKey().equals(oldPosterKey)) {
            fileStorageService.deleteFileAfterCommit(oldPosterKey);
        }
    }

    private void validateDateRange(FestivalRequestDto dto) {
        if (dto.getEndDate() != null && dto.getStartDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException(ERR_END_BEFORE_START);
        }
    }

    @Override
    @Transactional
    @EvictFestivalCaches
    @CacheEvict(value = "festivalDetail", key = "#festivalId")
    public void deleteFestival(Long festivalId) {
        // 소프트 삭제 — 타임테이블·부스·인증·좋아요 등 연관 데이터는 그대로 두고 목록·검색에서만
        // 제외한다. row가 물리적으로 남아있어 기존 FK 참조도 깨지지 않고, 휴지통에서 복구하면
        // 라인업까지 포함해 그대로 되돌아온다.
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findByIdAndDeletedAtIsNull, festivalId, "페스티벌");
        festival.softDelete();
    }

    @Override
    @Transactional
    @EvictFestivalCaches
    @CacheEvict(value = "festivalDetail", key = "#festivalId")
    public void restoreFestival(Long festivalId) {
        festivalRepository.restoreById(festivalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getDeletedFestivals() {
        return festivalRepository.findSoftDeleted().stream().map(this::toDto).toList();
    }

    @Override
    public String uploadPosterFile(MultipartFile file, LocalDate startDate) throws IOException {
        return fileStorageService.storeFestivalPoster(file, startDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getLikedFestivals(Long userId) {
        return festivalLikeRepository.findByUserId(userId, PageRequest.of(0, PageSize.MY_ACTIVITIES)).stream()
                .map(like -> toDto(like.getFestival()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDto> searchFestivals(String keyword) {
        String trimmed = keyword.trim();
        List<Festival> festivals = FullTextSearchValidator.isTooShortForFullText(trimmed)
                ? festivalRepository.findByTitleKeywordPaged(JpqlLikeEscaper.escape(trimmed), PageRequest.of(0, PageSize.SEARCH)).getContent()
                : festivalRepository.findByTitleKeyword(trimmed, PageSize.SEARCH);
        return festivals.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalResponseDto> getFestivalsAdminPage(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (keyword == null || keyword.isBlank()) {
            return festivalRepository.findAllByDeletedAtIsNull(
                    PageableFactory.orderByLatestStartDate(page, size))
                    .map(this::toDto);
        }
        return festivalRepository.findByTitleKeywordPaged(JpqlLikeEscaper.escape(keyword), pageable).map(this::toDto);
    }

    @Override
    public long getTotalCount() {
        return festivalRepository.countByDeletedAtIsNull();
    }
}
