package com.feple.feple_backend.badword;

import com.feple.feple_backend.badword.entity.BadWord;
import com.feple.feple_backend.badword.event.BadWordChangedEvent;
import com.feple.feple_backend.badword.repository.BadWordRepository;
import com.feple.feple_backend.badword.service.BadWordService;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BadWordServiceTest {

    @Mock BadWordRepository badWordRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks BadWordService badWordService;

    @Test
    void add_공백_입력_예외() {
        assertThatThrownBy(() -> badWordService.add("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금칙어를 입력해 주세요.");
    }

    @Test
    void add_50자_초과_예외() {
        String word = "a".repeat(51);

        assertThatThrownBy(() -> badWordService.add(word))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50자 이하");
    }

    @Test
    void add_중복_단어_예외() {
        given(badWordRepository.existsByWord("hello")).willReturn(true);

        assertThatThrownBy(() -> badWordService.add("hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된");
    }

    @Test
    void add_성공_소문자_변환_저장_이벤트_발행() {
        given(badWordRepository.existsByWord("hello")).willReturn(false);

        badWordService.add("HELLO");

        then(badWordRepository).should().save(any(BadWord.class));
        then(eventPublisher).should().publishEvent(any(BadWordChangedEvent.class));
    }

    @Test
    void delete_성공_이벤트_발행() {
        badWordService.delete(1L);

        then(badWordRepository).should().deleteById(1L);
        then(eventPublisher).should().publishEvent(any(BadWordChangedEvent.class));
    }
}
