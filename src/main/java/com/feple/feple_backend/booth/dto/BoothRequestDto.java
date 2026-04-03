package com.feple.feple_backend.booth.dto;

import com.feple.feple_backend.booth.entity.BoothType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothRequestDto {
    private String name;
    private BoothType boothType;
    private Double latitude;
    private Double longitude;
    private String description;
    private String imageUrl;
}
