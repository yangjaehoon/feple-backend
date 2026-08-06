package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostDraftRepository extends JpaRepository<PostDraft, Long> {

    // deleteById()는 대상이 없으면 예외를 던지므로, 임시저장이 없는 유저에 대한
    // "게시 후 임시저장 정리" 같은 best-effort 삭제에는 이 벌크 쿼리를 쓴다.
    @Modifying
    @Transactional
    @Query("DELETE FROM PostDraft d WHERE d.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
