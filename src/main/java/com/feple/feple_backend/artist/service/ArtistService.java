package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArtistService {

    /** 정렬 전략 맵 — 새 정렬 옵션 추가 시 이곳에만 추가 (Strategy Pattern) */
    private static final Map<String, Function<ArtistRepository, List<Artist>>> SORT_STRATEGIES = Map.of(
        "name",          repo -> repo.findAll(Sort.by(Sort.Direction.ASC,  "name")),
        "name_desc",     repo -> repo.findAll(Sort.by(Sort.Direction.DESC, "name")),
        "followers",     repo -> repo.findAll(Sort.by(Sort.Direction.DESC, "followerCount")),
        "followers_asc", repo -> repo.findAll(Sort.by(Sort.Direction.ASC,  "followerCount"))
    );

    private final ArtistRepository artistRepository;
    private final FileStorageService fileStorageService;
    private final ArtistProfileImageRepository artistImageRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    private ArtistResponseDto toDto(Artist artist) {
        return ArtistResponseDto.from(artist, fileStorageService.buildUrl(artist.getProfileImageKey()));
    }

    public Long createArtist(ArtistRequestDto dto){
        Artist artist = Artist.builder()
                .name(dto.getName())
                .nameEn(dto.getNameEn())
                .genre(dto.getGenre())
                .profileImageKey(dto.getProfileImageUrl())
                .build();

        return artistRepository.save(artist).getId();
    }

    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getAllArtists() {
        return artistRepository.findAll(PageRequest.of(0, 200,
                        Sort.by(Sort.Direction.DESC, "weeklyScore").and(Sort.by(Sort.Direction.ASC, "id"))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArtistResponseDto> searchArtists(String keyword) {
        return artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getAdminArtistList(String sort, String keyword, ArtistGenre genre) {
        Stream<ArtistResponseDto> stream = (keyword != null && !keyword.isBlank())
            ? artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream().map(this::toDto)
            : SORT_STRATEGIES.getOrDefault(sort, ArtistRepository::findAllByOrderByWeeklyScoreDescIdAsc)
                             .apply(artistRepository).stream().map(this::toDto);

        return (genre != null)
            ? stream.filter(a -> genre.getDisplayName().equals(a.getGenre())).toList()
            : stream.toList();
    }

    @Transactional(readOnly = true)
    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        return toDto(artist);
    }

    @Transactional(readOnly = true)
    public Page<ArtistResponseDto> getArtistsPage(int page, int size) {
        Page<Artist> result = artistRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return result.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ArtistRequestDto getArtistForEdit(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        return ArtistRequestDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .nameEn(artist.getNameEn())
                .genre(artist.getGenre())
                .profileImageUrl(fileStorageService.buildUrl(artist.getProfileImageKey()))
                .followerCount(artist.getFollowerCount())
                .build();
    }

    @Transactional
    public void updateArtist(Long id, ArtistRequestDto dto) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        String oldKey = artist.getProfileImageKey();
        String imageKey = dto.getProfileImageUrl() != null
                ? dto.getProfileImageUrl()
                : oldKey;
        artist.update(dto.getName(), dto.getNameEn(), dto.getGenre(), imageKey);
        if (dto.getProfileImageUrl() != null && !dto.getProfileImageUrl().equals(oldKey)) {
            fileStorageService.deleteFile(oldKey);
        }
    }

    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getTopArtists(int limit) {
        return artistRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "followerCount")))
                .getContent().stream().map(this::toDto).toList();
    }

    @Transactional
    public void updateArtistPhoto(Long id, String imageKey) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        String oldKey = artist.getProfileImageKey();
        artist.update(artist.getName(), artist.getNameEn(), artist.getGenre(), imageKey);
        if (imageKey != null && !imageKey.equals(oldKey)) {
            fileStorageService.deleteFile(oldKey);
        }
    }

    @Transactional
    public void batchUpdateNameEn(List<Long> ids, List<String> nameEns) {
        for (int i = 0; i < ids.size(); i++) {
            Artist artist = artistRepository.findById(ids.get(i)).orElse(null);
            if (artist != null) {
                String nameEn = (i < nameEns.size()) ? nameEns.get(i).trim() : "";
                artist.update(artist.getName(), nameEn.isEmpty() ? null : nameEn, artist.getGenre(), artist.getProfileImageKey());
            }
        }
    }

    @Transactional
    public void deleteArtist(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        String profileImageKey = artist.getProfileImageKey();

        // 아티스트 이미지 삭제 (S3 + DB, ArtistProfileImageLike는 cascade)
        List<ArtistProfileImage> images = artistImageRepository.findByArtist(artist);
        images.forEach(img -> fileStorageService.deleteFile(img.getImageUrl()));
        artistImageRepository.deleteAll(images);

        // 아티스트-페스티벌 연결, 팔로우 삭제
        artistFestivalRepository.deleteByArtistId(id);
        artistFollowRepository.deleteAll(artistFollowRepository.findByArtistId(id));

        // 게시글 삭제 (PostLike 먼저, Comment는 Post cascade)
        List<Post> artistPosts = postRepository.findByArtist(artist);
        for (Post post : artistPosts) {
            postLikeRepository.deleteByPostId(post.getId());
        }
        postRepository.deleteAll(artistPosts);

        artistRepository.deleteById(id);
        fileStorageService.deleteFile(profileImageKey);
    }
}
