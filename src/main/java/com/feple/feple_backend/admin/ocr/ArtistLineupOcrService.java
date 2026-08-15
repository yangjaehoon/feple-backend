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
        // 아티스트 수만큼 반복 비교되는 값이라 OCR 이름은 한 번만 정규화해 재사용한다
        // (매 비교마다 정규식을 다시 돌리지 않도록).
        String normalizedName = raw.name() != null ? normalize(raw.name()).toLowerCase() : null;

        Optional<Artist> exact = findExact(normalizedName, allArtists);
        if (exact.isPresent()) {
            Artist artist = exact.get();
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), raw.date(), conf);
        }
        List<Artist> partial = findPartial(normalizedName, allArtists);
        if (partial.size() == 1) {
            Artist artist = partial.get(0);
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), raw.date(), conf);
        }
        return new ArtistLineupOcrResult(raw.name(), null, null, raw.date(), conf);
    }

    // 정규화 후 두 명 이상의 아티스트와 동시에 "정확히" 일치하면(예: 서로 다른 아티스트의 이름이
    // 공백 차이로만 구분되던 경우) 아무나 하나를 고르지 않고 미매칭으로 남겨 관리자가 직접 확인하게 한다.
    private static Optional<Artist> findExact(String normalizedName, List<Artist> allArtists) {
        List<Artist> matches = allArtists.stream().filter(a -> matchesExact(a, normalizedName)).toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    private static List<Artist> findPartial(String normalizedName, List<Artist> allArtists) {
        return allArtists.stream().filter(a -> matchesPartial(a, normalizedName)).toList();
    }

    private static boolean matchesExact(Artist artist, String normalizedName) {
        return equalsNormalized(artist.getName(), normalizedName)
                || equalsNormalized(artist.getNameEn(), normalizedName)
                || artist.getAliases().stream().anyMatch(alias -> equalsNormalized(alias, normalizedName));
    }

    private static boolean matchesPartial(Artist artist, String normalizedName) {
        return containsNormalized(artist.getName(), normalizedName)
                || containsNormalized(artist.getNameEn(), normalizedName)
                || artist.getAliases().stream().anyMatch(alias -> containsNormalized(alias, normalizedName));
    }

    private static boolean equalsNormalized(String candidate, String normalizedName) {
        return candidate != null && normalizedName != null
                && normalize(candidate).toLowerCase().equals(normalizedName);
    }

    // OCR이 "LOCO (로꼬)"처럼 영문/한글을 함께 반환하는 경우 DB에 저장된 단일 이름이
    // OCR 원문보다 짧아 한쪽 방향 포함 검사만으로는 매칭되지 않으므로 양방향으로 확인한다.
    private static boolean containsNormalized(String candidate, String normalizedName) {
        if (candidate == null || normalizedName == null) return false;
        String normalizedCandidate = normalize(candidate).toLowerCase();
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
    // 전체 등록이 막히지 않도록 유효한 값만 골라 담는다. 같은 아티스트가 여러 행(예: DAY1/DAY2에
    // 중복 매칭)에 걸쳐 있으면 실제로 저장되는 쪽(=목록에서 먼저 나온 행, classifyArtistsToLink의
    // 중복 판정과 동일한 기준)의 날짜를 남기도록 첫 값만 채택한다.
    private Map<Long, LocalDate> parseArtistDates(List<LineupOcrArtistEntry> artists) {
        Map<Long, LocalDate> dates = new HashMap<>();
        for (LineupOcrArtistEntry entry : artists) {
            if (entry.date() == null || entry.date().isBlank()) continue;
            try {
                dates.putIfAbsent(entry.artistId(), LocalDate.parse(entry.date()));
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
