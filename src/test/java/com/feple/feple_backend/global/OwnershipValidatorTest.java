package com.feple.feple_backend.global;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OwnershipValidatorTest {

    @Test
    void checkOwner_본인이면_예외없음() {
        assertThatCode(() -> OwnershipValidator.checkOwner(1L, 1L, "게시글"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkOwner_본인아니면_AccessDeniedException() {
        assertThatThrownBy(() -> OwnershipValidator.checkOwner(1L, 2L, "게시글"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인의 게시글만 삭제할 수 있습니다.");
    }

    @Test
    void checkOwner_action_지정시_메시지에_반영() {
        assertThatThrownBy(() -> OwnershipValidator.checkOwner(1L, 2L, "댓글", "수정"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인의 댓글만 수정할 수 있습니다.");
    }

    @Test
    void checkOwner_ownerId_null이면_NullPointerException() {
        // ownerId는 항상 이미 로드된 엔티티에서 가져오므로 null이 될 수 없는 것이 현재 전제이나,
        // 이 전제가 깨질 경우의 실제 동작(NPE)을 문서화해둔다.
        assertThatThrownBy(() -> OwnershipValidator.checkOwner(null, 1L, "게시글"))
                .isInstanceOf(NullPointerException.class);
    }
}
