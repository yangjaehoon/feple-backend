package com.feple.feple_backend.ticketlink.dto;

import com.feple.feple_backend.ticketlink.entity.FestivalTicketLink;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketLinkResponseDto {
    private Long id;
    private String label;
    private String url;

    public static TicketLinkResponseDto from(FestivalTicketLink link) {
        return TicketLinkResponseDto.builder()
                .id(link.getId())
                .label(link.getLabel())
                .url(link.getUrl())
                .build();
    }
}
