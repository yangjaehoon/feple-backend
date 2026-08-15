package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistLineupOcrService {

    private final GeminiOcrClient geminiOcrClient;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;
    private final UnmatchedArtistSuggestionService suggestionService;

    public boolean isConfigured() {
        return geminiOcrClient.isConfigured();
    }

    public OcrParseResult<ArtistLineupOcrResult> parseArtistLineup(MultipartFile image, Integer year) throws IOException {
        OcrParseResult<LineupRawResult> raw = geminiOcrClient.parseLineup(image, year);
        // 이름마다 개별 조회하면 포스터 한 장(아티스트 20~60명)에 최대 60회 쿼리가 발생하므로
        // 전체 아티스트를 한 번만 조회해 메모리에서 매칭한다.
        List<Artist> allArtists = artistRepository.findAllWithAliases();
        List<ArtistLineupOcrResult> matched = raw.entries().stream()
                .map(entry -> matchArtist(entry, allArtists))
                .toList();
        return new OcrParseResult<>(matched, raw.truncated());
    }

    private ArtistLineupOcrResult matchArtist(LineupRawResult raw, List<Artist> allArtists) {
        int conf = raw.confidence() != null ? raw.confidence() : 0;
        Optional<Artist> exact = findExact(raw.name(), allArtists);
        if (exact.isPresent()) {
            Artist artist = exact.get();
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), raw.date(), conf);
        }
        List<Artist> partial = findPartial(raw.name(), allArtists);
        if (partial.size() == 1) {
            Artist artist = partial.get(0);
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), raw.date(), conf);
        }
        return new ArtistLineupOcrResult(raw.name(), null, null, raw.date(), conf);
    }

    private static Optional<Artist> findExact(String name, List<Artist> allArtists) {
        return allArtists.stream().filter(a -> matchesExact(a, name)).findFirst();
    }

    private static List<Artist> findPartial(String name, List<Artist> allArtists) {
        return allArtists.stream().filter(a -> matchesPartial(a, name)).toList();
    }

    private static boolean matchesExact(Artist artist, String name) {
        return equalsIgnoreCase(artist.getName(), name)
                || equalsIgnoreCase(artist.getNameEn(), name)
                || artist.getAliases().stream().anyMatch(alias -> equalsIgnoreCase(alias, name));
    }

    private static boolean matchesPartial(Artist artist, String name) {
        return containsIgnoreCase(artist.getName(), name)
                || containsIgnoreCase(artist.getNameEn(), name)
                || artist.getAliases().stream().anyMatch(alias -> containsIgnoreCase(alias, name));
    }

    private static boolean equalsIgnoreCase(String candidate, String name) {
        return candidate != null && name != null && normalize(candidate).equalsIgnoreCase(normalize(name));
    }

    // OCR이 "LOCO (로꼬)"처럼 영문/한글을 함께 반환하는 경우 DB에 저장된 단일 이름이
    // OCR 원문보다 짧아 한쪽 방향 포함 검사만으로는 매칭되지 않으므로 양방향으로 확인한다.
    private static boolean containsIgnoreCase(String candidate, String name) {
        if (candidate == null || name == null) return false;
        String normalizedCandidate = normalize(candidate).toLowerCase();
        String normalizedName = normalize(name).toLowerCase();
        return normalizedCandidate.contains(normalizedName) || normalizedName.contains(normalizedCandidate);
    }

    // OCR이 "다이나믹듀오"를 "다이나믹 듀오"처럼 띄어쓰기를 다르게 읽는 경우가 있어
    // 매칭 전에 공백을 제거해 비교한다.
    private static String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    // 아티스트별로 addArtistToFestival을 반복 호출하면 포스터 한 장(아티스트 20~60명)에
    // 최대 수백 회 쿼리가 발생하므로, festival/artist/기존 참여 여부를 한 번씩만 조회하는
    // linkArtistsToFestival로 일괄 처리한다.
    public LineupApplyResult applyArtistLineup(LineupOcrApplyRequestDto request) {
        List<LineupOcrArtistEntry> artists = request.artists();
        List<Long> artistIds = artists.stream().map(LineupOcrArtistEntry::artistId).toList();
        Map<Long, LocalDate> datesByArtistId = parseArtistDates(artists);
        ArtistFestivalService.LinkArtistsResult result = artistIds.isEmpty()
                ? new ArtistFestivalService.LinkArtistsResult(0, 0, 0)
                : artistFestivalService.linkArtistsToFestival(request.festivalId(), artistIds, datesByArtistId);
        if (request.unmatchedNames() != null) {
            suggestionService.saveAll(request.unmatchedNames());
        }
        return new LineupApplyResult(artistIds.size(), result.added(), result.duplicates(), result.errors());
    }

    // 날짜는 OCR 추출/관리자 수정 값이라 형식이 틀어질 수 있어, 개별 항목 파싱 실패로
    // 전체 등록이 막히지 않도록 유효한 값만 골라 담는다.
    private Map<Long, LocalDate> parseArtistDates(List<LineupOcrArtistEntry> artists) {
        Map<Long, LocalDate> dates = new HashMap<>();
        for (LineupOcrArtistEntry entry : artists) {
            if (entry.date() == null || entry.date().isBlank()) continue;
            try {
                dates.put(entry.artistId(), LocalDate.parse(entry.date()));
            } catch (DateTimeParseException e) {
                log.warn("라인업 OCR 날짜 파싱 실패, 무시하고 진행. artistId={}, date={}", entry.artistId(), entry.date());
            }
        }
        return dates;
    }

    public List<UnmatchedArtistSuggestionDto> getSuggestions() {
        return suggestionService.getAll();
    }

    public void deleteSuggestion(Long id) {
        suggestionService.delete(id);
    }
}
