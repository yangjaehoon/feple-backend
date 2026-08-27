package com.feple.feple_backend.post.service;

/** 게시글 태그 정규화. 대소문자·앞뒤 공백·선행 '#'만 다른 태그가 서로 다른 값으로 저장되지 않게 한다. */
final class PostTags {

    private PostTags() {}

    static String normalize(String tag) {
        String trimmed = tag == null ? "" : tag.trim().toLowerCase();
        return trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    }
}
