package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.entity.Post;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * "관계 id + keyword 유무에 따라 4가지 조회 쿼리 중 하나를 고른다"는 반복 로직을 한 곳에 모은다.
 * 하위 클래스(ArtistPostFilterStrategy/FestivalPostFilterStrategy)는 id 추출자와 4개의
 * 조회 메서드 참조만 넘기면 된다.
 */
abstract class AbstractPostRelationFilterStrategy implements PostRelationFilterStrategy {

    private final Function<PostAdminFilterDto, Long> idExtractor;
    private final TriFunction<Long, String, PageRequest, Page<Post>> byIdAndKeyword;
    private final BiFunction<Long, PageRequest, Page<Post>> byIdOnly;
    private final BiFunction<String, PageRequest, Page<Post>> byKeywordOnly;
    private final Function<PageRequest, Page<Post>> byNeither;

    protected AbstractPostRelationFilterStrategy(
            Function<PostAdminFilterDto, Long> idExtractor,
            TriFunction<Long, String, PageRequest, Page<Post>> byIdAndKeyword,
            BiFunction<Long, PageRequest, Page<Post>> byIdOnly,
            BiFunction<String, PageRequest, Page<Post>> byKeywordOnly,
            Function<PageRequest, Page<Post>> byNeither) {
        this.idExtractor = idExtractor;
        this.byIdAndKeyword = byIdAndKeyword;
        this.byIdOnly = byIdOnly;
        this.byKeywordOnly = byKeywordOnly;
        this.byNeither = byNeither;
    }

    @Override
    public Page<Post> findPosts(PostAdminFilterDto params, String keyword, PageRequest pageable) {
        boolean hasKeyword = !keyword.isEmpty();
        Long id = idExtractor.apply(params);
        if (id != null) {
            return hasKeyword ? byIdAndKeyword.apply(id, keyword, pageable) : byIdOnly.apply(id, pageable);
        }
        return hasKeyword ? byKeywordOnly.apply(keyword, pageable) : byNeither.apply(pageable);
    }
}
