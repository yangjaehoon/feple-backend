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
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SongRequestServiceImpl implements SongRequestService, SongRequestAdminService {

    private final SongRequestRepository songRequestRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final YoutubeSearchService youtubeSearchService;
    private final SongRepository songRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SongRequestResponseDto submit(Long artistId, Long userId, SubmitSongRequestDto dto) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");

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
        String nickname = resolveNickname(userId);
        return SongRequestResponseDto.from(saved, nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongRequestResponseDto> getMyAllRequests(Long userId) {
        String nickname = resolveNickname(userId);
        return songRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(r -> SongRequestResponseDto.from(r, nickname))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongRequestResponseDto> getMyRequests(Long artistId, Long userId) {
        String nickname = resolveNickname(userId);
        return songRequestRepository.findByArtistIdAndUserIdOrderByCreatedAtDesc(artistId, userId)
                .stream()
                .map(r -> SongRequestResponseDto.from(r, nickname))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SongRequestResponseDto> getRequestsPage(int page, int size, String status, String keyword) {
        PageRequest pageable = PageRequest.of(page, size);
        SongRequestStatus statusEnum = (status == null || status.isBlank() || status.equals("ALL"))
                ? null : SongRequestStatus.valueOf(status);
        String kw = (keyword == null || keyword.isBlank()) ? null : LikeEscaper.escape(keyword.trim());
        Page<SongRequest> requestsPage = songRequestRepository.findWithFilters(statusEnum, kw, pageable);
        Map<Long, String> nicknameMap = buildNicknameMap(
                requestsPage.getContent().stream().map(SongRequest::getUserId).toList());
        return requestsPage.map(r -> SongRequestResponseDto.from(r, nicknameMap.getOrDefault(r.getUserId(), "알 수 없음")));
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
        Map<Long, String> nicknameMap = buildNicknameMap(
                requests.stream().map(SongRequest::getUserId).toList());
        return requests.stream()
                .map(r -> SongRequestResponseDto.from(r, nicknameMap.getOrDefault(r.getUserId(), "알 수 없음")))
                .toList();
    }

    @Override
    @Transactional
    public void approve(Long requestId, String youtubeUrl) {
        SongRequest request = songRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("노래 요청을 찾을 수 없습니다."));

        request.approve();

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
                }
            }
        }

        songRequestRepository.save(request);
        eventPublisher.publishEvent(new SongRequestApprovedEvent(
                request.getUserId(), request.getSongTitle(), request.getArtistName()));
    }

    @Override
    @Transactional
    public void reject(Long requestId, String reason) {
        SongRequest request = songRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("노래 요청을 찾을 수 없습니다."));
        request.reject();
        songRequestRepository.save(request);
        eventPublisher.publishEvent(new SongRequestRejectedEvent(
                request.getUserId(), request.getSongTitle(), request.getArtistName(), reason));
    }

    private String resolveNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .filter(n -> n != null && !n.isBlank())
                .orElse("알 수 없음");
    }

    private Map<Long, String> buildNicknameMap(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : "알 수 없음"
                ));
    }
}
