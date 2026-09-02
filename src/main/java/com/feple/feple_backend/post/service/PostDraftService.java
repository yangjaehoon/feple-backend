package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostDraftRequestDto;
import com.feple.feple_backend.post.dto.PostDraftResponseDto;
import com.feple.feple_backend.post.entity.PostDraft;
import com.feple.feple_backend.post.entity.PostDraftContent;
import com.feple.feple_backend.post.repository.PostDraftRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostDraftService {

    private final PostDraftRepository postDraftRepository;

    @Transactional
    public void saveDraft(Long userId, PostDraftRequestDto dto) {
        PostDraftContent content = new PostDraftContent(
                dto.getTitle(), dto.getContent(), dto.getBoardType(), dto.isAnonymous(),
                dto.getArtistId(), dto.getFestivalId(), dto.getImageUrls());
        postDraftRepository.findById(userId).ifPresentOrElse(
                draft -> draft.update(content),
                () -> postDraftRepository.save(PostDraft.create(userId, content)));
    }

    public Optional<PostDraftResponseDto> getDraft(Long userId) {
        return postDraftRepository.findById(userId).map(PostDraftResponseDto::from);
    }

    @Transactional
    public void deleteDraft(Long userId) {
        postDraftRepository.deleteByUserId(userId);
    }
}
