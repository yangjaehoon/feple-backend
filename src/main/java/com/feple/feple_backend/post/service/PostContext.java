package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.entity.BoardType;

/** 게시글이 어느 게시판(일반 boardType / 아티스트 / 페스티벌)에 속하는지 나타내는 값 객체. */
record PostContext(BoardType boardType, Artist artist, Festival festival) {}
