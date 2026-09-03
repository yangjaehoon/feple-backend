package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostTag;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM PostTag t WHERE t.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostTag t WHERE t.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    // 태그로 게시글 검색(최신순 커서 페이징) — post_tag.tag 인덱스를 탄다.
    // 공개 조회라 삭제·블라인드된 게시글의 태그는 제외한다.
    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query("SELECT t FROM PostTag t JOIN t.post p WHERE t.tag = :tag "
            + "AND p.deletedAt IS NULL AND p.blinded = false ORDER BY p.id DESC")
    List<PostTag> findByTagOrderByPostIdDesc(@Param("tag") String tag, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query("SELECT t FROM PostTag t JOIN t.post p WHERE t.tag = :tag AND p.id < :cursor "
            + "AND p.deletedAt IS NULL AND p.blinded = false ORDER BY p.id DESC")
    List<PostTag> findByTagAndPostIdLessThanOrderByPostIdDesc(@Param("tag") String tag, @Param("cursor") Long cursor, Pageable pageable);
}
