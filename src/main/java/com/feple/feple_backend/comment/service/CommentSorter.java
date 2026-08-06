package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.entity.Comment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 최상위 댓글을 좋아요순으로 재배치하고, 각 댓글의 답글은 그 댓글 바로 아래에
 * 항상 작성순으로 붙인다 — 답글까지 좋아요순으로 섞으면 대화 흐름이 깨진다.
 */
final class CommentSorter {
    private CommentSorter() {}

    private static final Comparator<Comment> BY_CREATED_AT = Comparator.comparing(Comment::getCreatedAt);
    private static final Comparator<Comment> BY_LIKE_THEN_CREATED_AT =
            Comparator.comparing(Comment::getLikeCount, Comparator.reverseOrder()).thenComparing(BY_CREATED_AT);

    static List<Comment> sortByBest(List<Comment> flatComments) {
        Map<Long, List<Comment>> childrenByParentId = flatComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<Comment> roots = flatComments.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(BY_LIKE_THEN_CREATED_AT)
                .toList();

        List<Comment> result = new ArrayList<>();
        for (Comment root : roots) {
            result.add(root);
            appendChildren(root.getId(), childrenByParentId, result);
        }
        return result;
    }

    private static void appendChildren(Long parentId, Map<Long, List<Comment>> childrenByParentId, List<Comment> result) {
        for (Comment child : childrenByParentId.getOrDefault(parentId, List.of()).stream().sorted(BY_CREATED_AT).toList()) {
            result.add(child);
            appendChildren(child.getId(), childrenByParentId, result);
        }
    }
}
