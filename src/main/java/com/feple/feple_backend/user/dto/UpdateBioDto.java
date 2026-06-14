package com.feple.feple_backend.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateBioDto {
    @Size(max = 150, message = "자기소개는 150자 이하로 입력해주세요.")
    private String bio;
}
