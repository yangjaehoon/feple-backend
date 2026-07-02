package com.feple.feple_backend.nickname;

import com.feple.feple_backend.nickname.entity.NicknameRestriction;
import com.feple.feple_backend.nickname.event.NicknameRestrictionChangedEvent;
import com.feple.feple_backend.nickname.repository.NicknameRestrictionRepository;
import com.feple.feple_backend.nickname.service.NicknameRestrictionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NicknameRestrictionServiceTest {

    @Mock NicknameRestrictionRepository repository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks NicknameRestrictionService nicknameRestrictionService;

    @Test
    void add_공백_입력_예외() {
        assertThatThrownBy(() -> nicknameRestrictionService.add("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("단어를 입력해 주세요.");
    }

    @Test
    void add_50자_초과_예외() {
        String word = "a".repeat(51);

        assertThatThrownBy(() -> nicknameRestrictionService.add(word))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50자 이하");
    }

    @Test
    void add_중복_단어_예외() {
        given(repository.existsByWord("금지어")).willReturn(true);

        assertThatThrownBy(() -> nicknameRestrictionService.add("금지어"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된");
    }

    @Test
    void add_성공_저장_이벤트_발행() {
        given(repository.existsByWord("금지어")).willReturn(false);

        nicknameRestrictionService.add("금지어");

        then(repository).should().save(any(NicknameRestriction.class));
        then(eventPublisher).should().publishEvent(any(NicknameRestrictionChangedEvent.class));
    }

    @Test
    void delete_성공_이벤트_발행() {
        nicknameRestrictionService.delete(1L);

        then(repository).should().deleteById(1L);
        then(eventPublisher).should().publishEvent(any(NicknameRestrictionChangedEvent.class));
    }
}
