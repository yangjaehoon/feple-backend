package com.feple.feple_backend.global;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class LikeTogglerTest {

    @Test
    void 삭제된_행이_없으면_추가하고_true_반환() {
        boolean[] onLikeCalled = {false};

        boolean result = LikeToggler.toggle(() -> 0, () -> { throw new AssertionError("호출되면 안 됨"); },
                () -> onLikeCalled[0] = true);

        assertThat(result).isTrue();
        assertThat(onLikeCalled[0]).isTrue();
    }

    @Test
    void 삭제된_행이_있으면_취소하고_false_반환() {
        boolean[] onUnlikeCalled = {false};

        boolean result = LikeToggler.toggle(() -> 1, () -> onUnlikeCalled[0] = true,
                () -> { throw new AssertionError("호출되면 안 됨"); });

        assertThat(result).isFalse();
        assertThat(onUnlikeCalled[0]).isTrue();
    }

    @Test
    void 동시요청_경합으로_저장시_unique_제약_위반이어도_예외_없이_true_반환() {
        boolean result = LikeToggler.toggle(() -> 0, () -> { },
                () -> { throw new DataIntegrityViolationException("unique violation"); });

        assertThat(result).isTrue();
    }

    // ── toggleByExistence ────────────────────────────────────────────

    @Test
    void toggleByExistence_존재하지_않으면_추가하고_true_반환() {
        boolean[] onLikeCalled = {false};

        boolean result = LikeToggler.toggleByExistence(() -> false,
                () -> { throw new AssertionError("호출되면 안 됨"); },
                () -> onLikeCalled[0] = true);

        assertThat(result).isTrue();
        assertThat(onLikeCalled[0]).isTrue();
    }

    @Test
    void toggleByExistence_이미_존재하면_취소하고_false_반환() {
        boolean[] onUnlikeCalled = {false};

        boolean result = LikeToggler.toggleByExistence(() -> true,
                () -> onUnlikeCalled[0] = true,
                () -> { throw new AssertionError("호출되면 안 됨"); });

        assertThat(result).isFalse();
        assertThat(onUnlikeCalled[0]).isTrue();
    }

    @Test
    void toggleByExistence_저장시_unique_제약_위반이어도_예외_없이_true_반환() {
        boolean result = LikeToggler.toggleByExistence(() -> false, () -> { },
                () -> { throw new DataIntegrityViolationException("unique violation"); });

        assertThat(result).isTrue();
    }
}
