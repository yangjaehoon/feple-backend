package com.feple.feple_backend.artist.photo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPhotoRequestDto (
    @NotBlank(message = "오브젝트 키는 필수입니다.") String objectKey,
    @NotBlank(message = "Content-Type은 필수입니다.") String contentType,
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.") String title,
    String description,
    Boolean isAnonymous
){}
