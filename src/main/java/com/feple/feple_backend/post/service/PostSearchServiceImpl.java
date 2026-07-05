package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.repository.PostRepository;
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

    @Override
    public List<PostResponseDto> searchPosts(String keyword, String boardType) {
        String kw = keyword.trim();
        Optional<BoardType> type = parseBoardType(boardType);
        if (type.isPresent()) {
            return postRepository.searchPostsByBoardTypeAndTitleFullText(
                            type.get(), kw, PageRequest.of(0, PageSize.SEARCH))
                    .stream()
                    .map(PostResponseDto::from)
                    .toList();
        }
        return postRepository.searchPostsByTitleFullText(
                        kw, PageRequest.of(0, PageSize.SEARCH))
                .stream()
                .map(PostResponseDto::from)
                .toList();
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
