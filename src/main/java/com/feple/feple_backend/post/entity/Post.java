package com.feple.feple_backend.post.entity;

import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.user.domain.User;

import java.time.LocalDateTime;

public class Post {


    Long id;
    BoardType boardType;
    User author;
    String title;
    String content;
    LocalDateTime createdAt;

}
