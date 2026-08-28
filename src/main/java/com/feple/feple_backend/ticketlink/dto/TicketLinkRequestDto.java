package com.feple.feple_backend.ticketlink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class TicketLinkRequestDto {
    @Size(max = 100, message = "링크 이름은 100자 이하로 입력해주세요.")
    private String label;

    @NotBlank(message = "예매 링크 URL은 필수입니다.")
    @Size(max = 500, message = "URL은 500자 이하로 입력해주세요.")
    @Pattern(regexp = "^https?://.+", message = "http(s):// 로 시작하는 URL을 입력해주세요.")
    private String url;
}
