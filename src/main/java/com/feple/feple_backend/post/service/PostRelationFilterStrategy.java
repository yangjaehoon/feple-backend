package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 관리자 게시글 목록에서 특정 연관 엔티티(아티스트/페스티벌 등) 기준 필터를
 * 처리하는 전략. 새 관계 필터를 추가할 때 {@link PostAdminServiceImpl}을
 * 수정할 필요 없이 이 인터페이스의 구현체(스프링 빈)만 추가하면 된다.
 */
public interface PostRelationFilterStrategy {
    String filterKey();
    Page<Post> findPosts(PostAdminFilterDto params, boolean hasKeyword, String keyword, PageRequest pageable);
}
