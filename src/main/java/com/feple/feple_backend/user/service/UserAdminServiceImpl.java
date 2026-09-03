package com.feple.feple_backend.user.service;

import com.feple.feple_backend.admin.CurrentAdminProvider;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.PageableFactory;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    /** 관리자 푸시 대상 닉네임 검색 결과 노출 상한 (자동완성용이라 넉넉하지 않아도 됨) */
    private static final int NICKNAME_SEARCH_RESULT_LIMIT = 20;

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserCascadeDeleteService cascadeDeleteService;
    private final CurrentAdminProvider currentAdminProvider;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ArtistGalleryPhotoRepository artistGalleryPhotoRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getAdminUser(@NonNull Long id) {
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        return toAdminUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> searchByNickname(String nickname) {
        String keyword = JpqlLikeEscaper.escapeOrEmpty(nickname);
        if (keyword.isEmpty()) return List.of();
        Pageable pageable = PageableFactory.orderByLatestId(0, NICKNAME_SEARCH_RESULT_LIMIT);
        return userRepository.searchByNicknameContaining(keyword, pageable).stream()
                .map(this::toAdminUserDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPage(int page, int size, String keyword) {
        // keyword가 빈 문자열이면 LIKE '%%'로 치환되어 전체 조회와 동일한 결과를 반환한다
        // (findActiveByKeyword는 형제 메서드들과 달리 별도의 빈 키워드 분기가 쿼리에 없음)
        String kw = JpqlLikeEscaper.escapeOrEmpty(keyword);
        return userRepository.findActiveByKeyword(kw, PageableFactory.orderByLatestId(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPageSortedByReports(int page, int size, String keyword) {
        String kw = JpqlLikeEscaper.escapeOrEmpty(keyword);
        return userRepository.findAllOrderByTotalReportCountDesc(kw, PageRequest.of(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getBannedUsersPage(int page, int size, String keyword) {
        String kw = JpqlLikeEscaper.escapeOrEmpty(keyword);
        return userRepository.findBannedUsers(LocalDateTime.now(), kw, PageRequest.of(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return userRepository.countByDeletedAtIsNull();
    }

    @Override
    @Transactional
    public void bulkDeleteUsers(List<Long> ids) {
        // 관리자에 의한 삭제라 자진 탈퇴 사유(withdrawalReason)는 없음 — null로 남겨
        // PM이 탈퇴 사유 통계를 볼 때 사용자가 직접 응답한 것만 집계되게 함.
        userRepository.findAllById(ids).forEach(user -> cascadeDeleteService.delete(user, null, null));
    }

    // 삭제 대상 User는 어차피 cascadeDeleteService.delete()를 위해 로드해야 하므로,
    // 별도 조회 없이 그 김에 관리자 로그용 닉네임도 함께 반환한다.
    @Override
    public String adminDeleteUser(@NonNull Long id) {
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        String nickname = user.getNickname();
        cascadeDeleteService.delete(user, null, null);
        return nickname;
    }

    /**
     * 회원을 DB에서 완전히 제거한다(되돌릴 수 없음). 탈퇴 상태든 활성 상태든 한 번에 처리한다.
     * SUPER_ADMIN 전용이며, 실수로 실사용자 데이터를 날리지 않도록 <b>작성한 게시글·댓글·갤러리 사진이
     * 있으면 거부</b>한다 — 그런 계정은 남의 댓글·좋아요가 얽혀 있으므로 일반 삭제(익명 처리)를 써야 한다.
     * 로그인으로 재접근 불가능해진 OAuth 테스트 계정 정리 용도.
     */
    @Override
    public void hardDeleteUser(@NonNull Long id) {
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        // 소프트 삭제·블라인드된 글·댓글도 users FK를 잡고 있어 물리 삭제를 막으므로,
        // 가시성 필터 없는 존재 여부로 검사한다(countByUserId는 보이는 것만 세어 놓친다).
        if (postRepository.existsAnyByUserId(id)
                || commentRepository.existsAnyByUserId(id)
                || artistGalleryPhotoRepository.countByUploaderId(id) > 0) {
            throw new InvalidRequestException("작성한 게시글·댓글 또는 올린 갤러리 사진이 있어 완전 삭제할 수 없습니다. 일반 삭제(익명 처리)를 사용하세요.");
        }
        cascadeDeleteService.hardDelete(user);
    }

    @Override
    public void updateUserRole(Long userId, UserRole role) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        user.changeRole(role);
    }

    @Override
    public void banUser(Long userId, int days, String reason) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        user.ban(days, reason, currentAdminProvider.usernameOrNull());
    }

    @Override
    public void unbanUser(Long userId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        user.unban();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsersForExport() {
        List<UserResponseDto> result = new ArrayList<>();
        int page = 0;
        final int batchSize = 1000;
        Page<User> batch;
        do {
            batch = userRepository.findAllByDeletedAtIsNull(
                    PageableFactory.orderByLatestId(page++, batchSize));
            batch.forEach(u -> result.add(toAdminUserDto(u)));
        } while (batch.hasNext() && result.size() < PageSize.MAX_EXPORT_ROWS);
        return result;
    }

    private UserResponseDto toAdminUserDto(User user) {
        return UserResponseMapper.toAdminUserDto(user, fileStorageService);
    }
}
