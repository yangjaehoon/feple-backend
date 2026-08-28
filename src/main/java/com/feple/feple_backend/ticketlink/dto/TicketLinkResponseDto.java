package com.feple.feple_backend.ticketlink.dto;

import com.feple.feple_backend.ticketlink.entity.FestivalTicketLink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
