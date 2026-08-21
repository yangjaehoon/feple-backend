package com.feple.feple_backend.ticketlink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.ticketlink.dto.TicketLinkRequestDto;
import com.feple.feple_backend.ticketlink.entity.FestivalTicketLink;
import com.feple.feple_backend.ticketlink.repository.FestivalTicketLinkRepository;
import com.feple.feple_backend.ticketlink.service.FestivalTicketLinkService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalTicketLinkServiceTest {

    @Mock FestivalTicketLinkRepository ticketLinkRepository;
    @Mock FestivalRepository festivalRepository;

    @InjectMocks FestivalTicketLinkService ticketLinkService;

    @Test
    void createTicketLink_페스티벌_없거나_삭제됐으면_예외() {
        given(festivalRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

        TicketLinkRequestDto dto = TicketLinkRequestDto.builder()
                .label("인터파크")
                .url("https://tickets.interpark.com/example")
                .build();

        assertThatThrownBy(() -> ticketLinkService.createTicketLink(1L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createTicketLink_성공_링크_ID_반환() {
        Festival festival = mock(Festival.class);
        given(festivalRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(festival));

        FestivalTicketLink savedLink = mock(FestivalTicketLink.class);
        given(savedLink.getId()).willReturn(5L);
        given(ticketLinkRepository.save(any(FestivalTicketLink.class))).willReturn(savedLink);

        TicketLinkRequestDto dto = TicketLinkRequestDto.builder()
                .label("인터파크")
                .url("https://tickets.interpark.com/example")
                .build();

        Long result = ticketLinkService.createTicketLink(1L, dto);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void deleteTicketLink_링크_없으면_예외() {
        given(ticketLinkRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketLinkService.deleteTicketLink(1L, 10L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteTicketLink_다른_페스티벌_예외() {
        FestivalTicketLink link = mock(FestivalTicketLink.class);
        given(link.getFestivalId()).willReturn(99L);
        given(ticketLinkRepository.findById(10L)).willReturn(Optional.of(link));

        assertThatThrownBy(() -> ticketLinkService.deleteTicketLink(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 티켓 링크가 아닙니다.");
    }

    @Test
    void deleteTicketLink_성공() {
        FestivalTicketLink link = mock(FestivalTicketLink.class);
        given(link.getFestivalId()).willReturn(1L);
        given(ticketLinkRepository.findById(10L)).willReturn(Optional.of(link));

        ticketLinkService.deleteTicketLink(1L, 10L);

        then(ticketLinkRepository).should().delete(link);
    }

    @Test
    void getTicketLinks_페스티벌_링크_목록_반환() {
        FestivalTicketLink link = mock(FestivalTicketLink.class);
        given(link.getLabel()).willReturn("예스24");
        given(link.getUrl()).willReturn("https://ticket.yes24.com/example");
        given(ticketLinkRepository.findByFestivalId(1L)).willReturn(List.of(link));

        var result = ticketLinkService.getTicketLinks(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLabel()).isEqualTo("예스24");
        assertThat(result.get(0).getUrl()).isEqualTo("https://ticket.yes24.com/example");
    }
}
