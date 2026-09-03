package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM PostImage pi WHERE pi.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    // 회원 완전 삭제(hardDelete) — 행을 지우기 전에 S3 객체를 정리하기 위해 키를 먼저 조회한다.
    @Query("SELECT pi.imageKey FROM PostImage pi WHERE pi.post.id IN :postIds")
    List<String> findImageKeysByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostImage pi WHERE pi.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);
}
