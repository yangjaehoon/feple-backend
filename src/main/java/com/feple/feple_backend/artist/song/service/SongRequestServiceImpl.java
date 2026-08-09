package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.SaveSongDto;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.event.SongRequestApprovedEvent;
import com.feple.feple_backend.artist.song.event.SongRequestRejectedEvent;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.UserNicknameLookup;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import com.feple.feple_backend.global.exception.ConflictException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongRequestServiceImpl implements SongRequestService, SongRequestAdminService {

    private final SongRequestRepository songRequestRepository;
    private final ArtistRepository artistRepository;
    private final UserNicknameLookup nicknameResolver;
    private final YoutubeSearchService youtubeSearchService;
    private final SongAdminService songAdminService;
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
        return toResponseList(songRequestRepository.findByUserIdOrderByCreatedAtDesc(userId), nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongRequestResponseDto> getMyRequests(Long artistId, Long userId) {
        String nickname = nicknameResolver.lookup(userId);
        return toResponseList(songRequestRepository.findByArtistIdAndUserIdOrderByCreatedAtDesc(artistId, userId), nickname);
    }

    private List<SongRequestResponseDto> toResponseList(List<SongRequest> requests, String nickname) {
        return requests.stream()
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
    public long getTotalCount() {
        return songRequestRepository.count();
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
        // YouTube API 호출(외부 I/O)을 DB 접근보다 먼저 수행해 트랜잭션 내 커넥션 점유 시간을 최소화
        Optional<YoutubeVideoDto> videoOpt = fetchVideoIfProvided(youtubeUrl);

        SongRequest request = EntityLoader.getOrThrow(songRequestRepository::findById, requestId, "노래 요청");
        requirePending(request);
        request.approve();

        boolean songSaved = false;
        if (youtubeUrl != null && !youtubeUrl.isBlank()) {
            request.updateYoutubeUrl(youtubeUrl);
            songSaved = trySaveSong(request, videoOpt);
        }
        publishApprovedEvent(request);
        return songSaved;
    }

    private Optional<YoutubeVideoDto> fetchVideoIfProvided(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.isBlank()) return Optional.empty();
        return youtubeSearchService.fetchVideoByUrl(youtubeUrl);
    }

    // 곡 생성 로직(중복 체크+저장)은 SongAdminService가 소유 — 여기서 재구현하지 않고 위임한다.
    private boolean trySaveSong(SongRequest request, Optional<YoutubeVideoDto> videoOpt) {
        if (videoOpt.isEmpty()) return false;

        YoutubeVideoDto video = videoOpt.get();
        SaveSongDto dto = new SaveSongDto();
        dto.setYoutubeVideoId(video.getVideoId());
        dto.setTitle(video.getTitle());
        dto.setThumbnailUrl(video.getThumbnailUrl());
        return songAdminService.saveSongIfAbsent(request.getArtistId(), dto).isPresent();
    }

    private void publishApprovedEvent(SongRequest request) {
        eventPublisher.publishEvent(new SongRequestApprovedEvent(
                request.getUserId(), request.getArtistId(), request.getSongTitle(),
                request.getArtistName(), request.getArtistNameEn()));
    }

    // 알 수 없는 status 값은 PENDING으로 조용히 대체하면 "필터를 걸었는데 계속 대기중 건만
    // 보인다"는 원인 불명 증상으로 이어진다 — null(findWithFilters에서 전체 조회로 해석됨)로
    // 폴백하고 흔적을 로그에 남긴다.
    private SongRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equals("ALL")) return null;
        try {
            return SongRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 노래 요청 상태값이라 전체 조회로 대체합니다: {}", status);
            return null;
        }
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void reject(Long requestId, String reason) {
        SongRequest request = EntityLoader.getOrThrow(songRequestRepository::findById, requestId, "노래 요청");
        requirePending(request);
        request.reject();
        eventPublisher.publishEvent(new SongRequestRejectedEvent(
                request.getUserId(), request.getArtistId(), request.getSongTitle(), request.getArtistName(), reason));
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        songRequestRepository.deleteByUserId(userId);
    }

    // 이중 클릭·요청 재시도로 동일 요청이 두 번 승인/반려되며 알림이 중복 발송되는 것을 방지
    private void requirePending(SongRequest request) {
        if (!request.isPending()) {
            throw new IllegalArgumentException("이미 처리된 노래 요청입니다.");
        }
    }
}
