package com.feple.feple_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateNicknameDto {
    @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
    @Size(min = 2, max = 8, message = "닉네임은 2자 이상 8자 이하로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$", message = "닉네임은 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.")
    private String nickname;
}
