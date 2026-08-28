package com.feple.feple_backend.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "search_log", indexes = {
        @Index(name = "idx_search_log_created_at", columnList = "created_at"),
        @Index(name = "idx_search_log_keyword", columnList = "keyword")
})
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String keyword;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static SearchLog of(String keyword) {
        SearchLog log = new SearchLog();
        log.keyword = keyword;
        return log;
    }
}
