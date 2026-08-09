package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.BoardType;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 임시저장은 작성 중인 글을 그대로 보존하는 용도라 PostRequestDto와 달리 필수값 검증을 하지 않는다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDraftRequestDto {

    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
    private String content;

    private BoardType boardType;

    private boolean anonymous;

    @Size(max = PostRequestDto.MAX_IMAGES, message = "이미지는 최대 " + PostRequestDto.MAX_IMAGES + "장까지 첨부할 수 있습니다.")
    @Builder.Default
    private List<@Size(max = 255, message = "이미지 URL이 너무 깁니다.") String> imageUrls = List.of();

    private Long artistId;

    private Long festivalId;
}
