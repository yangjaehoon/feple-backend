package com.feple.feple_backend.post.service;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.FullTextSearchValidator;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchServiceImpl implements PostSearchService {

    private final PostRepository postRepository;
    private final BlockedContentFilter blockedContentFilter;
    private final FileStorageService fileStorageService;

    @Override
    public List<PostResponseDto> searchPosts(String keyword, String boardType, Long viewerId) {
        String kw = keyword.trim();
        Optional<BoardType> type = parseBoardType(boardType);
        // 최종 노출 개수(SEARCH)가 아니라 넉넉한 풀(SEARCH_POOL)을 조회한다 — 다음 페이지
        // 개념이 없는 단발성 목록이라, 차단 필터링으로 결과가 줄어들어도 재요청으로 보충할
        // 방법이 없다.
        PageRequest pageable = PageRequest.of(0, PageSize.SEARCH_POOL);
        List<PostResponseDto> results = FullTextSearchValidator.isTooShortForFullText(kw)
                ? searchByTitleLike(type, kw, pageable)
                : searchByTitleFullText(type, kw, pageable);
        List<PostResponseDto> filtered = blockedContentFilter.excludeBlocked(results, viewerId, PostResponseDto::getUserId);
        return filtered.stream().limit(PageSize.SEARCH).toList();
    }

    private List<PostResponseDto> searchByTitleFullText(Optional<BoardType> type, String kw, PageRequest pageable) {
        return type.isPresent()
                ? postRepository.searchPostsByBoardTypeAndTitleFullText(type.get(), kw, pageable)
                        .stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList()
                : postRepository.searchPostsByTitleFullText(kw, pageable)
                        .stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList();
    }

    private List<PostResponseDto> searchByTitleLike(Optional<BoardType> type, String kw, PageRequest pageable) {
        String escaped = JpqlLikeEscaper.escape(kw);
        return type.isPresent()
                ? postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(type.get(), escaped, pageable)
                        .stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList()
                : postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(escaped, pageable)
                        .stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList();
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
