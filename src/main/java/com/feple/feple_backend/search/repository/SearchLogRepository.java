package com.feple.feple_backend.search.repository;

import com.feple.feple_backend.search.entity.SearchLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query(value = """
            SELECT keyword, COUNT(*) AS cnt
            FROM search_log
            WHERE created_at >= :since
            GROUP BY keyword
            ORDER BY cnt DESC
            LIMIT :lim
            """, nativeQuery = true)
    List<Object[]> findTopKeywordsSince(@Param("since") LocalDateTime since, @Param("lim") int lim);

    // 한 번에 전체를 지우면 하나의 커넥션·트랜잭션을 오래 붙잡는다 — LIMIT으로 잘라 여러 번 커밋
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM search_log WHERE created_at < :cutoff LIMIT :batchSize", nativeQuery = true)
    int deleteByCreatedAtBeforeBatch(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
