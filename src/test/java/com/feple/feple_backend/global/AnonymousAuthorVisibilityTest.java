package com.feple.feple_backend.global;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnonymousAuthorVisibilityTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_VIEWER_ID = 2L;

    @Test
    void 익명이_아니면_userId와_certified를_그대로_노출() {
        var result = AnonymousAuthorVisibility.resolve(
                false, AUTHOR_ID, new AnonymousAuthorVisibility.Request(true, OTHER_VIEWER_ID, false));

        assertThat(result.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(result.certified()).isTrue();
    }

    @Test
    void 익명이면_타인에게는_userId와_certified_모두_숨김() {
        var result = AnonymousAuthorVisibility.resolve(
                true, AUTHOR_ID, new AnonymousAuthorVisibility.Request(true, OTHER_VIEWER_ID, false));

        assertThat(result.authorId()).isNull();
        assertThat(result.certified()).isFalse();
    }

    @Test
    void 익명이어도_본인이_조회하면_userId_노출() {
        var result = AnonymousAuthorVisibility.resolve(
                true, AUTHOR_ID, new AnonymousAuthorVisibility.Request(false, AUTHOR_ID, false));

        assertThat(result.authorId()).isEqualTo(AUTHOR_ID);
    }

    @Test
    void 익명이어도_revealAuthorId면_userId_노출() {
        var result = AnonymousAuthorVisibility.resolve(
                true, AUTHOR_ID, new AnonymousAuthorVisibility.Request(false, null, true));

        assertThat(result.authorId()).isEqualTo(AUTHOR_ID);
    }

    @Test
    void 익명이고_viewerId가_null이면_userId_숨김() {
        var result = AnonymousAuthorVisibility.resolve(
                true, AUTHOR_ID, new AnonymousAuthorVisibility.Request(false, null, false));

        assertThat(result.authorId()).isNull();
    }
}
