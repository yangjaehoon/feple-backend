package com.feple.feple_backend.support;

import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;

import java.time.LocalDateTime;

public final class TestEntityFactory {

    private TestEntityFactory() {}

    public static User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    public static Post freePost(Long id, User author) {
        return Post.builder()
                .id(id).title("제목" + id).content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    public static Post freePostWithLikeCount(Long id, User author, int likeCount) {
        return Post.builder()
                .id(id).title("제목" + id).content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(likeCount).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    public static Post freePostWithCommentCount(Long id, User author, int commentCount) {
        return Post.builder()
                .id(id).title("제목" + id).content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(0).scrapCount(0).commentCount(commentCount)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    public static Post freePostWithScrapCount(Long id, User author, int scrapCount) {
        return Post.builder()
                .id(id).title("제목" + id).content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(0).scrapCount(scrapCount)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
