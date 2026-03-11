package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.DuplicateArtistFestivalException;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.dto.ArtistScheduleResponse;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistFestivalService {

    private final ArtistFestivalRepository artistFestivalRepository;
    private final FestivalRepository festivalRepository;
    private final ArtistRepository artistRepository;
    private final FileStorageService fileStorageService;

    public List<ArtistScheduleResponse> getArtistSchedule(Long artistId) {
        return artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(artistId)
                .stream()
                .map(af -> {
                    Festival festival = af.getFestival();
                    List<ArtistScheduleResponse.CoArtistInfo> coArtists =
                            artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festival.getId())
                                    .stream()
                                    .filter(other -> !other.getArtist().getId().equals(artistId))
                                    .map(other -> ArtistScheduleResponse.CoArtistInfo.builder()
                                            .artistId(other.getArtist().getId())
                                            .artistName(other.getArtist().getName())
                                            .profileImageUrl(fileStorageService.buildUrl(other.getArtist().getProfileImageKey()))
                                            .build())
                                    .toList();
                    return ArtistScheduleResponse.builder()
                            .festivalId(festival.getId())
                            .title(festival.getTitle())
                            .description(festival.getDescription())
                            .location(festival.getLocation())
                            .startDate(festival.getStartDate())
                            .endDate(festival.getEndDate())
                            .posterUrl(fileStorageService.buildUrl(festival.getPosterKey()))
                            .eventType(festival.getEventType())
                            .coArtists(coArtists)
                            .build();
                })
                .toList();
    }

    public List<ArtistFestivalResponse> getArtistFestivals(Long festivalId) {
        return artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public Long addArtistToFestival(Long festivalId, ArtistFestivalCreateRequest request) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("Festival not found: " + festivalId));

        Artist artist = artistRepository.findById(request.getArtistId())
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + request.getArtistId()));

        if (artistFestivalRepository.existsByFestivalIdAndArtistId(festivalId, request.getArtistId())) {
            throw new DuplicateArtistFestivalException();
        }

        ArtistFestival artistFestival = ArtistFestival.builder()
                .festival(festival)
                .artist(artist)
                .lineupOrder(request.getLineupOrder())
                .stageName(request.getStageName())
                .build();

        ArtistFestival saved = artistFestivalRepository.save(artistFestival);
        return saved.getId();
    }

    @Transactional
    public void updateArtistFestival(Long festivalId, Long artistFestivalId,
                                     Integer lineupOrder, String stageName) {
        ArtistFestival af = artistFestivalRepository.findById(artistFestivalId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 없습니다."));
        if (!af.getFestival().getId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }
        af.updateLineup(lineupOrder, stageName);
    }

    @Transactional
    public void removeArtistFromFestival(Long festivalId, Long artistFestivalId) {
        ArtistFestival artistFestival = artistFestivalRepository.findById(artistFestivalId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 없습니다."));

        if (!artistFestival.getFestival().getId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }

        artistFestivalRepository.delete(artistFestival);
    }


    private ArtistFestivalResponse toResponse(ArtistFestival af) {
        return ArtistFestivalResponse.builder()
                .artistFestivalId(af.getId())
                .artistId(af.getArtist().getId())
                .artistName(af.getArtist().getName())
                .artistGenre(af.getArtist().getGenre() != null ? af.getArtist().getGenre().getDisplayName() : null)
                .profileImageUrl(fileStorageService.buildUrl(af.getArtist().getProfileImageKey()))
                .lineupOrder(af.getLineupOrder())
                .stageName(af.getStageName())
                .build();
    }
}