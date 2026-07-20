package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.dto.RegisterPhotoRequestDto;
import com.feple.feple_backend.artist.photo.dto.UpdatePhotoRequestDto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistGalleryPhotoServiceTest {

    @Mock ArtistGalleryPhotoRepository artistGalleryPhotoRepository;
    @Mock S3PresignService s3PresignService;
    @Mock S3ObjectVerificationService s3ObjectVerificationService;
    @Mock FileStorageService fileStorageService;
    @Mock ArtistGalleryPhotoLikeRepository artistGalleryPhotoLikeRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ArtistGalleryPhotoService service;

    private Artist artist(Long id) {
        return Artist.builder().id(id).name("아티스트" + id).build();
    }

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id).build();
    }

    private ArtistGalleryPhoto photo(Long id, Artist artist, User uploader) {
        ArtistGalleryPhoto photo = new ArtistGalleryPhoto(artist, uploader,
                "artist-photos/" + artist.getId() + "/key.jpg", "image/jpeg", "title", "desc", false);
        ReflectionTestUtils.setField(photo, "id", id);
        return photo;
    }

    // ── generateUploadUrl ────────────────────────────────────────────────

    @Test
    void 업로드_URL_생성시_아티스트_prefix_포함된_키_생성() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        S3PresignedUrlResult expected = new S3PresignedUrlResult("https://upload-url", "key");
        given(s3PresignService.presignPut(keyCaptor.capture(), eq("image/jpeg"))).willReturn(expected);

        S3PresignedUrlResult result = service.generateUploadUrl(1L, "jpg", "image/jpeg");

        assertThat(result).isEqualTo(expected);
        assertThat(keyCaptor.getValue()).startsWith("artist-photos/1/").endsWith(".jpg");
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    void 등록_정상_흐름() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        String objectKey = "artist-photos/1/key.jpg";
        RegisterPhotoRequestDto req = new RegisterPhotoRequestDto(objectKey, "image/jpeg", "title", "desc", false);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(userRepository.findById(10L)).willReturn(Optional.of(uploader));
        ArtistGalleryPhoto saved = photo(100L, artist, uploader);
        given(artistGalleryPhotoRepository.save(any(ArtistGalleryPhoto.class))).willReturn(saved);
        given(s3PresignService.presignGetUrl(saved.getS3Key())).willReturn("https://get-url");

        ArtistGalleryPhotoResponseDto result = service.register(1L, req, 10L);

        assertThat(result.photoId()).isEqualTo(100L);
        assertThat(result.url()).isEqualTo("https://get-url");
    }

    @Test
    void 등록_objectKey가_null이면_예외() {
        RegisterPhotoRequestDto req = new RegisterPhotoRequestDto(null, "image/jpeg", "title", "desc", false);

        assertThatThrownBy(() -> service.register(1L, req, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키");
    }

    @Test
    void 등록_objectKey가_다른_아티스트_prefix면_예외() {
        RegisterPhotoRequestDto req = new RegisterPhotoRequestDto(
                "artist-photos/999/key.jpg", "image/jpeg", "title", "desc", false);

        assertThatThrownBy(() -> service.register(1L, req, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키");
    }

    @Test
    void 등록_S3에_파일_없으면_예외() {
        RegisterPhotoRequestDto req = new RegisterPhotoRequestDto(
                "artist-photos/1/key.jpg", "image/jpeg", "title", "desc", false);
        willThrow(new IllegalArgumentException("업로드된 파일을 찾을 수 없습니다."))
                .given(s3ObjectVerificationService).verifyImageObject(any());

        assertThatThrownBy(() -> service.register(1L, req, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    void 등록_허용되지_않는_컨텐츠타입이면_예외() {
        RegisterPhotoRequestDto req = new RegisterPhotoRequestDto(
                "artist-photos/1/key.txt", "text/plain", "title", "desc", false);
        willThrow(new IllegalArgumentException("허용되지 않는 파일 형식입니다. 이미지 파일만 등록할 수 있습니다."))
                .given(s3ObjectVerificationService).verifyImageObject(any());

        assertThatThrownBy(() -> service.register(1L, req, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않는");
    }

    // ── list ─────────────────────────────────────────────────────────────

    @Test
    void 목록_조회시_좋아요한_사진_반영() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p1 = photo(1L, artist, uploader);
        ArtistGalleryPhoto p2 = photo(2L, artist, uploader);
        given(artistGalleryPhotoRepository.findByArtist_IdOrderByLikeCountDescCreatedAtDesc(1L))
                .willReturn(List.of(p1, p2));
        given(artistGalleryPhotoLikeRepository.findLikedPhotoIds(99L, List.of(1L, 2L)))
                .willReturn(Set.of(1L));
        given(s3PresignService.presignGetUrl(any())).willReturn("https://url");

        List<ArtistGalleryPhotoResponseDto> result = service.list(1L, 99L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isLiked()).isTrue();
        assertThat(result.get(1).isLiked()).isFalse();
    }

    @Test
    void 목록_조회시_currentUserId_없으면_좋아요_조회_생략() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        given(artistGalleryPhotoRepository.findByArtist_IdOrderByLikeCountDescCreatedAtDesc(1L))
                .willReturn(List.of(photo(1L, artist, uploader)));
        given(s3PresignService.presignGetUrl(any())).willReturn("https://url");

        List<ArtistGalleryPhotoResponseDto> result = service.list(1L, null);

        assertThat(result.get(0).isLiked()).isFalse();
        verify(artistGalleryPhotoLikeRepository, never()).findLikedPhotoIds(any(), any());
    }

    // ── delete ───────────────────────────────────────────────────────────

    @Test
    void 삭제_본인_사진이면_성공() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));

        service.delete(5L, 10L);

        verify(artistGalleryPhotoLikeRepository).deleteByPhotoId(5L);
        verify(artistGalleryPhotoRepository).delete(p);
        verify(fileStorageService).deleteFileAfterCommit(p.getS3Key());
    }

    @Test
    void 삭제_타인_사진이면_예외() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));

        assertThatThrownBy(() -> service.delete(5L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인이 업로드한");
    }

    @Test
    void 삭제_존재하지_않는_사진이면_예외() {
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L, 10L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── update ───────────────────────────────────────────────────────────

    @Test
    void 수정_본인_사진이면_제목_설명_갱신() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));

        service.update(5L, 10L, new UpdatePhotoRequestDto("새 제목", "새 설명"));

        assertThat(p.getTitle()).isEqualTo("새 제목");
        assertThat(p.getDescription()).isEqualTo("새 설명");
    }

    @Test
    void 수정_타인_사진이면_예외() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));

        assertThatThrownBy(() -> service.update(5L, 999L, new UpdatePhotoRequestDto("t", "d")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인이 업로드한");
    }

    // ── getPhoto ─────────────────────────────────────────────────────────

    @Test
    void 단건조회_로그인유저_좋아요여부_포함() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));
        given(s3PresignService.presignGetUrl(p.getS3Key())).willReturn("https://url");
        given(artistGalleryPhotoLikeRepository.existsByPhoto_IdAndUser_Id(5L, 99L)).willReturn(true);

        ArtistGalleryPhotoResponseDto result = service.getPhoto(5L, 99L);

        assertThat(result.isLiked()).isTrue();
    }

    @Test
    void 단건조회_비로그인이면_좋아요_false() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));
        given(s3PresignService.presignGetUrl(p.getS3Key())).willReturn("https://url");

        ArtistGalleryPhotoResponseDto result = service.getPhoto(5L, null);

        assertThat(result.isLiked()).isFalse();
        verify(artistGalleryPhotoLikeRepository, never()).existsByPhoto_IdAndUser_Id(any(), any());
    }

    // ── removeByUser ─────────────────────────────────────────────────────

    @Test
    void 회원탈퇴시_좋아요_카운트_감소후_삭제() {
        service.removeByUser(10L);

        verify(artistGalleryPhotoLikeRepository).decrementLikeCountByUserId(10L);
        verify(artistGalleryPhotoLikeRepository).deleteByUserId(10L);
    }

    // ── toggleLike ───────────────────────────────────────────────────────

    @Test
    void 좋아요토글_기존에_없으면_추가() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        User liker = user(20L);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));
        given(userRepository.findById(20L)).willReturn(Optional.of(liker));
        given(artistGalleryPhotoLikeRepository.deleteByPhotoIdAndUserId(5L, 20L)).willReturn(0);

        boolean result = service.toggleLike(5L, 20L);

        assertThat(result).isTrue();
        verify(artistGalleryPhotoLikeRepository).saveAndFlush(any(ArtistGalleryPhotoLike.class));
        verify(artistGalleryPhotoRepository).incrementLikeCount(5L);
    }

    @Test
    void 좋아요토글_기존에_있으면_취소() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        User liker = user(20L);
        given(artistGalleryPhotoRepository.findById(5L)).willReturn(Optional.of(p));
        given(userRepository.findById(20L)).willReturn(Optional.of(liker));
        given(artistGalleryPhotoLikeRepository.deleteByPhotoIdAndUserId(5L, 20L)).willReturn(1);

        boolean result = service.toggleLike(5L, 20L);

        assertThat(result).isFalse();
        verify(artistGalleryPhotoRepository).decrementLikeCount(5L);
        verify(artistGalleryPhotoLikeRepository, never()).saveAndFlush(any());
    }
}
