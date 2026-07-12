package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequestDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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

    public OcrParseResult<ArtistLineupOcrResult> parseArtistLineup(MultipartFile image) throws IOException {
        OcrParseResult<LineupRawResult> raw = geminiOcrClient.parseLineup(image);
        List<ArtistLineupOcrResult> matched = raw.entries().stream().map(this::matchArtist).toList();
        return new OcrParseResult<>(matched, raw.truncated());
    }

    private ArtistLineupOcrResult matchArtist(LineupRawResult raw) {
        int conf = raw.confidence() != null ? raw.confidence() : 0;
        Optional<Artist> exact = artistRepository.findExactByNameIgnoreCase(raw.name());
        if (exact.isPresent()) {
            Artist artist = exact.get();
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), conf);
        }
        List<Artist> partial = artistRepository.findByNameOrNameEnContainingIgnoreCase(raw.name());
        if (partial.size() == 1) {
            Artist artist = partial.get(0);
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), conf);
        }
        return new ArtistLineupOcrResult(raw.name(), null, null, conf);
    }

    // @Transactional 제거: addArtistToFestival(ConflictException 발생 시)이 외부 트랜잭션을
    // rollback-only로 마킹해 UnexpectedRollbackException이 발생하는 것을 방지.
    // 각 addArtistToFestival 호출은 자신의 독립 트랜잭션을 사용함.
    public LineupApplyResult applyArtistLineup(LineupOcrApplyRequestDto request) {
        int added = 0;
        int duplicates = 0;
        for (Long id : request.artistIds()) {
            try {
                ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
                req.setArtistId(id);
                artistFestivalService.addArtistToFestival(request.festivalId(), req);
                added++;
            } catch (ConflictException e) {
                duplicates++;
            }
        }
        if (request.unmatchedNames() != null) {
            suggestionService.saveAll(request.unmatchedNames());
        }
        return new LineupApplyResult(request.artistIds().size(), added, duplicates);
    }

    public List<UnmatchedArtistSuggestionDto> getSuggestions() {
        return suggestionService.getAll();
    }

    public void deleteSuggestion(Long id) {
        suggestionService.delete(id);
    }
}
