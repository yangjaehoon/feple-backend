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

        Optional<Artist> matched = resolveMatch(normalizedName, allArtists);
        if (matched.isPresent()) {
            Artist artist = matched.get();
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), raw.date(), conf);
        }
        return new ArtistLineupOcrResult(raw.name(), null, null, raw.date(), conf);
    }

    // 이름(한글/영문)을 별명보다, 정확 일치를 부분 일치보다 먼저 확인한다. 앞 단계에서 후보가
    // 하나라도 나오면(유일하게 좁혀지든 여러 명과 겹쳐 애매하든) 그 결과가 최종이고 다음 단계로
    // 넘어가지 않는다. 그렇지 않으면 이름 쪽에서 여러 아티스트와 애매하게 겹친 OCR 텍스트가
    // 별명 쪽에서 우연히 하나로만 좁혀졌다는 이유로 조용히 엉뚱한 아티스트로 확정될 수 있다.
    private static Optional<Artist> resolveMatch(String normalizedName, List<Artist> allArtists) {
        List<Artist> nameExact = allArtists.stream().filter(a -> matchesNameExact(a, normalizedName)).toList();
        if (!nameExact.isEmpty()) return uniqueOrEmpty(nameExact);

        List<Artist> namePartial = allArtists.stream().filter(a -> matchesNamePartial(a, normalizedName)).toList();
        if (!namePartial.isEmpty()) return uniqueOrEmpty(namePartial);

        List<Artist> aliasExact = allArtists.stream().filter(a -> matchesAliasExact(a, normalizedName)).toList();
        if (!aliasExact.isEmpty()) return uniqueOrEmpty(aliasExact);

        List<Artist> aliasPartial = allArtists.stream().filter(a -> matchesAliasPartial(a, normalizedName)).toList();
        return uniqueOrEmpty(aliasPartial);
    }

    private static Optional<Artist> uniqueOrEmpty(List<Artist> candidates) {
        return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
    }

    private static boolean matchesNameExact(Artist artist, String normalizedName) {
        return equalsNormalized(artist.getName(), normalizedName) || equalsNormalized(artist.getNameEn(), normalizedName);
    }

    private static boolean matchesNamePartial(Artist artist, String normalizedName) {
        return containsNormalized(artist.getName(), normalizedName) || containsNormalized(artist.getNameEn(), normalizedName);
    }

    private static boolean matchesAliasExact(Artist artist, String normalizedName) {
        return artist.getAliases().stream().anyMatch(alias -> equalsNormalized(alias, normalizedName));
    }

    private static boolean matchesAliasPartial(Artist artist, String normalizedName) {
        return artist.getAliases().stream().anyMatch(alias -> containsNormalized(alias, normalizedName));
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
