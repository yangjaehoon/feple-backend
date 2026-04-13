package com.feple.feple_backend.booth.dto;

import com.feple.feple_backend.booth.entity.BoothType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothRequestDto {
    @NotBlank(message = "부스 이름은 필수입니다.")
    private String name;
    @NotNull(message = "부스 유형은 필수입니다.")
    private BoothType boothType;
    @NotNull(message = "위도는 필수입니다.")
    private Double latitude;
    @NotNull(message = "경도는 필수입니다.")
    private Double longitude;
    private String description;
    private String imageUrl;
}
