package com.feple.feple_backend.booth.dto;

import com.feple.feple_backend.booth.entity.Booth;
import com.feple.feple_backend.booth.entity.BoothType;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothResponseDto {
    private Long id;
    private String name;
    private BoothType boothType;
    private String boothTypeName;
    private Double latitude;
    private Double longitude;
    private String description;
    private String imageUrl;

    public static BoothResponseDto from(Booth booth) {
        return BoothResponseDto.builder()
                .id(booth.getId())
                .name(booth.getName())
                .boothType(booth.getBoothType())
                .boothTypeName(booth.getBoothType() != null ? booth.getBoothType().getDisplayName() : null)
                .latitude(booth.getLatitude())
                .longitude(booth.getLongitude())
                .description(booth.getDescription())
                .imageUrl(booth.getImageUrl())
                .build();
    }
}
