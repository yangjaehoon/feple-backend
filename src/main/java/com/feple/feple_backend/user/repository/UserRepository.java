package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndOauthId(AuthProvider provider, String oauthId);

    // point += delta를 원자적 UPDATE로 처리 — 동시 이벤트(좋아요/댓글/게시글 작성 등)가
    // REQUIRES_NEW로 병렬 커밋될 때 read-modify-write로 인한 lost update를 방지한다.
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE users SET point = GREATEST(0, point + :delta) WHERE id = :id", nativeQuery = true)
    void addPointAtomically(@Param("id") Long id, @Param("delta") int delta);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND " +
           "(LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')")
    Page<User> findActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    java.util.List<User> findTop5ByDeletedAtIsNullOrderByIdDesc();

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.nickname = :nickname AND u.deletedAt IS NULL")
    boolean existsByNickname(@Param("nickname") String nickname);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.nickname = :nickname AND u.id <> :id AND u.deletedAt IS NULL")
    boolean existsByNicknameAndIdNot(@Param("nickname") String nickname, @Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.nickname = :nickname AND u.deletedAt IS NULL")
    Optional<User> findByNicknameAndNotDeleted(@Param("nickname") String nickname);

    long countByDeletedAtIsNull();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', u.createdAt), COUNT(u) FROM User u " +
           "WHERE u.createdAt >= :from AND u.createdAt < :to GROUP BY FUNCTION('DATE', u.createdAt)")
    List<Object[]> countGroupByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT DISTINCT user_id FROM post WHERE created_at >= :start AND created_at < :end AND deleted_at IS NULL
                UNION
                SELECT DISTINCT user_id FROM comment WHERE created_at >= :start AND created_at < :end AND deleted_at IS NULL
            ) AS active_users
            """, nativeQuery = true)
    Long countActiveUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.bannedUntil IS NOT NULL AND u.bannedUntil > :now " +
           "AND (:keyword = '' OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
           "ORDER BY u.bannedUntil DESC")
    Page<User> findBannedUsers(@Param("now") LocalDateTime now,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    @Query(value = """
            SELECT u.* FROM users u
            LEFT JOIN (
                SELECT p.user_id AS uid, COUNT(pr.id) AS cnt
                FROM post p JOIN post_report pr ON pr.post_id = p.id
                GROUP BY p.user_id
            ) post_r ON post_r.uid = u.id
            LEFT JOIN (
                SELECT c.user_id AS uid, COUNT(cr.id) AS cnt
                FROM comment c JOIN comment_report cr ON cr.comment_id = c.id
                GROUP BY c.user_id
            ) com_r ON com_r.uid = u.id
            LEFT JOIN (
                SELECT ap.uploader_user_id AS uid, COUNT(apr.id) AS cnt
                FROM artist_photos ap JOIN artist_photo_report apr ON apr.photo_id = ap.id
                GROUP BY ap.uploader_user_id
            ) photo_r ON photo_r.uid = u.id
            WHERE u.deleted_at IS NULL
              AND (:keyword = '' OR u.nickname LIKE CONCAT('%', :keyword, '%') ESCAPE '!' OR u.email LIKE CONCAT('%', :keyword, '%') ESCAPE '!')
            ORDER BY (COALESCE(post_r.cnt, 0) + COALESCE(com_r.cnt, 0) + COALESCE(photo_r.cnt, 0)) DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM users u
            WHERE u.deleted_at IS NULL
              AND (:keyword = '' OR u.nickname LIKE CONCAT('%', :keyword, '%') ESCAPE '!' OR u.email LIKE CONCAT('%', :keyword, '%') ESCAPE '!')
            """,
            nativeQuery = true)
    Page<User> findAllOrderByTotalReportCountDesc(@Param("keyword") String keyword, Pageable pageable);
}
