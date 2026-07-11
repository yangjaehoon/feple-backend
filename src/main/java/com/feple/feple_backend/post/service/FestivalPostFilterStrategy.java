package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FestivalPostFilterStrategy implements PostRelationFilterStrategy {

    private final PostRepository postRepository;

    @Override
    public String filterKey() { return "FESTIVAL"; }

    @Override
    public Page<Post> findPosts(PostAdminFilterDto params, boolean hasKeyword, String keyword, PageRequest pageable) {
        Long festivalId = params.festivalId();
        if (festivalId != null) {
            return hasKeyword
                    ? postRepository.findByFestivalIdAndTitleLikeOrderByCreatedAtDesc(festivalId, keyword, pageable)
                    : postRepository.findByFestivalIdOrderByCreatedAtDesc(festivalId, pageable);
        }
        return hasKeyword
                ? postRepository.findByFestivalIsNotNullAndTitleLikeOrderByCreatedAtDesc(keyword, pageable)
                : postRepository.findByFestivalIsNotNullOrderByCreatedAtDesc(pageable);
    }
}
