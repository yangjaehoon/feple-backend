package com.feple.feple_backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.post.dto.PostDraftRequestDto;
import com.feple.feple_backend.post.dto.PostDraftResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.PostDraft;
import com.feple.feple_backend.post.entity.PostDraftContent;
import com.feple.feple_backend.post.repository.PostDraftRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostDraftServiceTest {

    @Mock PostDraftRepository postDraftRepository;

    @InjectMocks PostDraftService postDraftService;

    // ── saveDraft ────────────────────────────────────────────────────

    @Test
    void 기존_임시저장_없으면_새로_생성() {
        given(postDraftRepository.findById(1L)).willReturn(Optional.empty());
        PostDraftRequestDto dto = PostDraftRequestDto.builder()
                .title("제목").content("내용").boardType(BoardType.FREE)
                .imageUrls(List.of("posts/1/a.jpg")).build();

        postDraftService.saveDraft(1L, dto);

        verify(postDraftRepository).save(any(PostDraft.class));
    }

    @Test
    void 기존_임시저장_있으면_덮어씀() {
        PostDraft existing = PostDraft.create(1L, new PostDraftContent("이전 제목", null, null, false, null, null, List.of()));
        given(postDraftRepository.findById(1L)).willReturn(Optional.of(existing));
        PostDraftRequestDto dto = PostDraftRequestDto.builder()
                .title("새 제목").content("새 내용").boardType(BoardType.MATE).build();

        postDraftService.saveDraft(1L, dto);

        assertThat(existing.getTitle()).isEqualTo("새 제목");
        verify(postDraftRepository, never()).save(any());
    }

    // ── getDraft ─────────────────────────────────────────────────────

    @Test
    void 임시저장_없으면_빈값() {
        given(postDraftRepository.findById(1L)).willReturn(Optional.empty());

        Optional<PostDraftResponseDto> result = postDraftService.getDraft(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void 임시저장_있으면_이미지키_리스트로_변환() {
        PostDraft draft = PostDraft.create(1L,
                new PostDraftContent("제목", null, null, false, null, null, List.of("posts/1/a.jpg", "posts/1/b.jpg")));
        given(postDraftRepository.findById(1L)).willReturn(Optional.of(draft));

        Optional<PostDraftResponseDto> result = postDraftService.getDraft(1L);

        assertThat(result).isPresent();
        assertThat(result.get().imageUrls()).containsExactly("posts/1/a.jpg", "posts/1/b.jpg");
    }

    // ── deleteDraft ──────────────────────────────────────────────────

    @Test
    void 임시저장_삭제() {
        postDraftService.deleteDraft(1L);

        verify(postDraftRepository).deleteByUserId(1L);
    }
}
