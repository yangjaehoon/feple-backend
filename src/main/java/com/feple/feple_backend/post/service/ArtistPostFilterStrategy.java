package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.repository.PostRepository;
import org.springframework.stereotype.Component;

@Component
class ArtistPostFilterStrategy extends AbstractPostRelationFilterStrategy {

    ArtistPostFilterStrategy(PostRepository postRepository) {
        super(PostAdminFilterDto::artistId,
                postRepository::findByArtistIdAndTitleLikeOrderByCreatedAtDesc,
                postRepository::findByArtistIdOrderByCreatedAtDesc,
                postRepository::findByArtistIsNotNullAndTitleLikeOrderByCreatedAtDesc,
                postRepository::findByArtistIsNotNullOrderByCreatedAtDesc);
    }

    @Override
    public String filterKey() { return "ARTIST"; }
}
