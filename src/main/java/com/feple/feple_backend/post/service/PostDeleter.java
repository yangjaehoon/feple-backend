package com.feple.feple_backend.post.service;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.repository.PostImageRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.post.repository.PostTagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글을 자식 레코드까지 물리 삭제하는 순서를 한 곳에서 관리한다({@link CommentDeleter}의 게시글 판).
 * 벌크 DELETE라 {@code @SQLDelete}(소프트 삭제)를 우회한 하드 삭제다. 회원 완전 삭제(hardDelete)
 * 경로 전용 — 일반 게시글 삭제는 소프트 삭제(휴지통)를 유지한다.
 *
 * <p>post의 자기 도메인 자식만 다룬다. 댓글·알림 등 다른 도메인 참조는 호출부
 * ({@link PostCascadeDeleteServiceImpl})가 해당 도메인 서비스로 먼저 정리한다.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class PostDeleter {

    private final PostImageRepository postImageRepository;
    private final PostTagRepository postTagRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;
    private final FileStorageService fileStorageService;

    public void deleteByIds(List<Long> postIds) {
        if (postIds.isEmpty()) return;
        // 행을 지우기 전에 S3 객체 정리를 예약한다(커밋 후 실행) — 안 하면 이미지가 영구 고아가 된다.
        for (String imageKey : postImageRepository.findImageKeysByPostIds(postIds)) {
            fileStorageService.deleteFileAfterCommit(imageKey);
        }
        postImageRepository.deleteByPostIds(postIds);
        postTagRepository.deleteByPostIds(postIds);
        postLikeRepository.deleteByPostIds(postIds);
        postScrapRepository.deleteByPostIds(postIds);
        postReportRepository.deleteByPostIds(postIds);
        postRepository.hardDeleteByIds(postIds);
    }
}
