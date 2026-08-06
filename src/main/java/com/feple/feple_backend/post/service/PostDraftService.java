package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostDraftRequestDto;
import com.feple.feple_backend.post.dto.PostDraftResponseDto;
import com.feple.feple_backend.post.entity.PostDraft;
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
        String imageKeysCsv = PostDraft.toImageKeysCsv(dto.getImageUrls());
        postDraftRepository.findById(userId).ifPresentOrElse(
                draft -> draft.update(dto.getTitle(), dto.getContent(), dto.getBoardType(), dto.isAnonymous(),
                        dto.getArtistId(), dto.getFestivalId(), imageKeysCsv),
                () -> postDraftRepository.save(PostDraft.builder()
                        .userId(userId)
                        .title(dto.getTitle())
                        .content(dto.getContent())
                        .boardType(dto.getBoardType())
                        .anonymous(dto.isAnonymous())
                        .artistId(dto.getArtistId())
                        .festivalId(dto.getFestivalId())
                        .imageKeysCsv(imageKeysCsv)
                        .build()));
    }

    public Optional<PostDraftResponseDto> getDraft(Long userId) {
        return postDraftRepository.findById(userId).map(PostDraftResponseDto::from);
    }

    @Transactional
    public void deleteDraft(Long userId) {
        postDraftRepository.deleteByUserId(userId);
    }
}
