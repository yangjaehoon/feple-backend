package com.feple.feple_backend.userblock.repository;

import com.feple.feple_backend.userblock.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM UserBlock b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
    boolean existsByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserBlock b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
    int deleteByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Query("SELECT b FROM UserBlock b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId ORDER BY b.createdAt DESC")
    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(@Param("blockerId") Long blockerId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserBlock b WHERE b.blocker.id = :userId OR b.blocked.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :blockerId")
    List<Long> findBlockedIdsByBlockerId(@Param("blockerId") Long blockerId);
}
