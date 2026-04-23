package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.photo.entity.ArtistImage;
import com.feple.feple_backend.artist.photo.repository.ArtistImageRepository;
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

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final FileStorageService fileStorageService;
    private final ArtistImageRepository artistImageRepository;
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
        List<ArtistResponseDto> result;
        if (keyword != null && !keyword.isBlank()) {
            result = artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream()
                    .map(this::toDto).toList();
        } else {
            result = switch (sort == null ? "" : sort) {
                case "name" -> artistRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                        .map(this::toDto).toList();
                case "name_desc" -> artistRepository.findAll(Sort.by(Sort.Direction.DESC, "name")).stream()
                        .map(this::toDto).toList();
                case "followers" -> artistRepository.findAll(Sort.by(Sort.Direction.DESC, "followerCount")).stream()
                        .map(this::toDto).toList();
                case "followers_asc" -> artistRepository.findAll(Sort.by(Sort.Direction.ASC, "followerCount")).stream()
                        .map(this::toDto).toList();
                default -> artistRepository.findAllByOrderByWeeklyScoreDescIdAsc().stream()
                        .map(this::toDto).toList();
            };
        }
        if (genre != null) {
            result = result.stream()
                    .filter(a -> genre.getDisplayName().equals(a.getGenre()))
                    .toList();
        }
        return result;
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

        // 아티스트 이미지 삭제 (S3 + DB, ArtistImageLike는 cascade)
        List<ArtistImage> images = artistImageRepository.findByArtist(artist);
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
