package com.feple.feple_backend.notice.dto;

import com.feple.feple_backend.global.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeRequestDto {

    @NotBlank(message = ValidationMessages.TITLE_REQUIRED)
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = ValidationMessages.CONTENT_REQUIRED)
    @Size(max = 10000, message = "내용은 10000자 이하여야 합니다.")
    private String content;

    private boolean pinned;

    private boolean sendNotification;
}
