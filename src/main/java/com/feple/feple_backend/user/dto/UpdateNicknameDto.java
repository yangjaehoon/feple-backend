package com.feple.feple_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateNicknameDto {
    @NotBlank(message= "닉네임은 비어 있을 수 없습니다.")
    @Size(min = 2, max=10, message="닉네임은 2~20자 사이어야합니다.")
    private String nickname;
}
