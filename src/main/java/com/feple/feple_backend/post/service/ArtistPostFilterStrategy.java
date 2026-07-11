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
class ArtistPostFilterStrategy implements PostRelationFilterStrategy {

    private final PostRepository postRepository;

    @Override
    public String filterKey() { return "ARTIST"; }

    @Override
    public Page<Post> findPosts(PostAdminFilterDto params, boolean hasKeyword, String keyword, PageRequest pageable) {
        Long artistId = params.artistId();
        if (artistId != null) {
            return hasKeyword
                    ? postRepository.findByArtistIdAndTitleLikeOrderByCreatedAtDesc(artistId, keyword, pageable)
                    : postRepository.findByArtistIdOrderByCreatedAtDesc(artistId, pageable);
        }
        return hasKeyword
                ? postRepository.findByArtistIsNotNullAndTitleLikeOrderByCreatedAtDesc(keyword, pageable)
                : postRepository.findByArtistIsNotNullOrderByCreatedAtDesc(pageable);
    }
}
