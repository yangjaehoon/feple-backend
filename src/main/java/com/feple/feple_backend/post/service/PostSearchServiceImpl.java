package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.FullTextSearchValidator;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchServiceImpl implements PostSearchService {

    private final PostRepository postRepository;
    private final BlockedContentFilter blockedContentFilter;

    @Override
    public List<PostResponseDto> searchPosts(String keyword, String boardType, Long viewerId) {
        String kw = keyword.trim();
        Optional<BoardType> type = parseBoardType(boardType);
        PageRequest pageable = PageRequest.of(0, PageSize.SEARCH);
        List<PostResponseDto> results = FullTextSearchValidator.isTooShortForFullText(kw)
                ? searchByTitleLike(type, kw, pageable)
                : searchByTitleFullText(type, kw, pageable);
        return blockedContentFilter.excludeBlocked(results, viewerId, PostResponseDto::getUserId);
    }

    private List<PostResponseDto> searchByTitleFullText(Optional<BoardType> type, String kw, PageRequest pageable) {
        return type.isPresent()
                ? postRepository.searchPostsByBoardTypeAndTitleFullText(type.get(), kw, pageable)
                        .stream().map(PostResponseDto::from).toList()
                : postRepository.searchPostsByTitleFullText(kw, pageable)
                        .stream().map(PostResponseDto::from).toList();
    }

    private List<PostResponseDto> searchByTitleLike(Optional<BoardType> type, String kw, PageRequest pageable) {
        String escaped = JpqlLikeEscaper.escape(kw);
        return type.isPresent()
                ? postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(type.get(), escaped, pageable)
                        .stream().map(PostResponseDto::from).toList()
                : postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(escaped, pageable)
                        .stream().map(PostResponseDto::from).toList();
    }

    private Optional<BoardType> parseBoardType(String filter) {
        if (filter == null) return Optional.empty();
        return switch (filter) {
            case "FREE" -> Optional.of(BoardType.FREE);
            case "MATE" -> Optional.of(BoardType.MATE);
            default     -> Optional.empty();
        };
    }
}
