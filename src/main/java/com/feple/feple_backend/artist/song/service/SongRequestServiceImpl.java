package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import com.feple.feple_backend.artist.song.event.SongRequestApprovedEvent;
import com.feple.feple_backend.artist.song.event.SongRequestRejectedEvent;
import com.feple.feple_backend.artist.song.entity.Song;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.UserNicknameLookup;
import com.feple.feple_backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SongRequestServiceImpl implements SongRequestService, SongRequestAdminService {

    private final SongRequestRepository songRequestRepository;
    private final ArtistRepository artistRepository;
    private final UserNicknameLookup nicknameResolver;
    private final YoutubeSearchService youtubeSearchService;
    private final SongRepository songRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SongRequestResponseDto submit(Long artistId, Long userId, SubmitSongRequestDto dto) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");

        boolean alreadyRequested = songRequestRepository
                .existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
                        artistId, userId, dto.getSongTitle(), SongRequestStatus.PENDING);
        if (alreadyRequested) {
            throw new ConflictException("이미 요청한 곡입니다.");
        }

        SongRequest request = SongRequest.builder()
                .artist(artist)
                .userId(userId)
                .songTitle(dto.getSongTitle())
                .youtubeUrl(dto.getYoutubeUrl())
                .build();

        SongRequest saved = songRequestRepository.save(request);
        String nickname = nicknameResolver.lookup(userId);
        return SongRequestResponseDto.from(saved, nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongRequestResponseDto> getMyAllRequests(Long userId) {
        String nickname = nicknameResolver.lookup(userId);
        return songRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(r -> SongRequestResponseDto.from(r, nickname))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongRequestResponseDto> getMyRequests(Long artistId, Long userId) {
        String nickname = nicknameResolver.lookup(userId);
        return songRequestRepository.findByArtistIdAndUserIdOrderByCreatedAtDesc(artistId, userId)
                .stream()
                .map(r -> SongRequestResponseDto.from(r, nickname))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SongRequestResponseDto> getRequestsPage(int page, int size, String status, String keyword) {
        PageRequest pageable = PageRequest.of(page, size);
        SongRequestStatus statusFilter = parseStatus(status);
        String kw = JpqlLikeEscaper.escapeOrNull(keyword);
        Page<SongRequest> requestsPage = songRequestRepository.findWithFilters(statusFilter, kw, pageable);
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(requestsPage.getContent(), SongRequest::getUserId);
        return requestsPage.map(r -> SongRequestResponseDto.from(r, nicknameMap.getOrDefault(r.getUserId(), UserNicknameLookup.UNKNOWN)));
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingCount() {
        return songRequestRepository.countByStatus(SongRequestStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongRequestResponseDto> getPendingRequests(Long artistId) {
        List<SongRequest> requests = songRequestRepository
                .findByArtistIdAndStatusOrderByCreatedAtDesc(artistId, SongRequestStatus.PENDING);
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(requests, SongRequest::getUserId);
        return requests.stream()
                .map(r -> SongRequestResponseDto.from(r, nicknameMap.getOrDefault(r.getUserId(), UserNicknameLookup.UNKNOWN)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongRequest> getPendingPreview(int limit) {
        return songRequestRepository.findByStatusOrderByCreatedAtDesc(SongRequestStatus.PENDING, PageRequest.of(0, limit));
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public boolean approveAndMaybeSaveSong(Long requestId, String youtubeUrl) {
        SongRequest request = EntityLoader.getOrThrow(songRequestRepository::findById, requestId, "노래 요청");

        request.approve();

        boolean songSaved = false;
        if (youtubeUrl != null && !youtubeUrl.isBlank()) {
            request.updateYoutubeUrl(youtubeUrl);
            Optional<YoutubeVideoDto> videoOpt = youtubeSearchService.fetchVideoByUrl(youtubeUrl);
            if (videoOpt.isPresent()) {
                YoutubeVideoDto video = videoOpt.get();
                Artist artist = request.getArtist();
                if (!songRepository.existsByYoutubeVideoIdAndArtistId(video.getVideoId(), artist.getId())) {
                    Song song = Song.builder()
                            .title(video.getTitle())
                            .youtubeVideoId(video.getVideoId())
                            .thumbnailUrl(video.getThumbnailUrl())
                            .artist(artist)
                            .build();
                    songRepository.save(song);
                    songSaved = true;
                }
            }
        }

        eventPublisher.publishEvent(new SongRequestApprovedEvent(
                request.getUserId(), request.getArtistId(), request.getSongTitle(),
                request.getArtistName(), request.getArtistNameEn()));
        return songSaved;
    }

    private SongRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equals("ALL")) return null;
        try {
            return SongRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return SongRequestStatus.PENDING;
        }
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void reject(Long requestId, String reason) {
        SongRequest request = EntityLoader.getOrThrow(songRequestRepository::findById, requestId, "노래 요청");
        request.reject();
        eventPublisher.publishEvent(new SongRequestRejectedEvent(
                request.getUserId(), request.getArtistId(), request.getSongTitle(), request.getArtistName(), reason));
    }

}
