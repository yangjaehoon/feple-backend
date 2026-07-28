package com.feple.feple_backend.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EntityLoaderTest {

    @Test
    void getOrThrow_존재하면_값_반환() {
        String result = EntityLoader.getOrThrow(id -> Optional.of("엔티티" + id), 1L, "게시글");

        assertThat(result).isEqualTo("엔티티1");
    }

    @Test
    void getOrThrow_없으면_예외_메시지에_id_포함() {
        assertThatThrownBy(() -> EntityLoader.getOrThrow(id -> Optional.empty(), 99L, "게시글"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("게시글")
                .hasMessageContaining("99");
    }

    @Test
    void requireBelongsToFestival_일치하면_통과() {
        EntityLoader.requireBelongsToFestival(1L, 1L, "부스가");
    }

    @Test
    void requireBelongsToFestival_불일치하면_예외() {
        assertThatThrownBy(() -> EntityLoader.requireBelongsToFestival(1L, 2L, "부스가"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 부스가 아닙니다.");
    }
}
