package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.repository.PostRepository;
import org.springframework.stereotype.Component;

@Component
class FestivalPostFilterStrategy extends AbstractPostRelationFilterStrategy {

    FestivalPostFilterStrategy(PostRepository postRepository) {
        super(PostAdminFilterDto::festivalId,
                postRepository::findByFestivalIdAndTitleLikeOrderByCreatedAtDesc,
                postRepository::findByFestivalIdOrderByCreatedAtDesc,
                postRepository::findByFestivalIsNotNullAndTitleLikeOrderByCreatedAtDesc,
                postRepository::findByFestivalIsNotNullOrderByCreatedAtDesc);
    }

    @Override
    public String filterKey() { return "FESTIVAL"; }
}
