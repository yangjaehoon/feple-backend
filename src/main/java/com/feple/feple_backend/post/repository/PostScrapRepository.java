package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostScrap;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM PostScrap ps WHERE ps.user.id = :userId AND ps.post.id = :postId")
    int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN TRUE ELSE FALSE END FROM PostScrap ps WHERE ps.user.id = :userId AND ps.post.id = :postId")
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    // 스크랩 행은 대상 글이 소프트 삭제·블라인드돼도 정리되지 않으므로, 공개 목록은 여기서
    // 가시성을 걸러야 한다. 아래 countByUserId도 같은 조건을 써야 목록 길이와 숫자가 어긋나지 않는다.
    String VISIBLE_POST = " AND ps.post.deletedAt IS NULL AND ps.post.blinded = false";

    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query("SELECT ps FROM PostScrap ps WHERE ps.user.id = :userId" + VISIBLE_POST + " ORDER BY ps.id DESC")
    List<PostScrap> findByUserIdOrderByIdDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(ps) FROM PostScrap ps WHERE ps.user.id = :userId" + VISIBLE_POST)
    long countByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET scrap_count = GREATEST(scrap_count - 1, 0) WHERE id IN (SELECT post_id FROM post_scrap WHERE user_id = :userId)", nativeQuery = true)
    void decrementPostScrapCountByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostScrap ps WHERE ps.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 회원 완전 삭제(hardDelete) — 이 유저의 글에 달린 (다른 유저 포함) 모든 스크랩 물리 삭제.
    @Modifying
    @Transactional
    @Query("DELETE FROM PostScrap ps WHERE ps.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);
}
