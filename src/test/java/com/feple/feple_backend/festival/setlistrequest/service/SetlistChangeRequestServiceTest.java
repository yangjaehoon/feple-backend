package com.feple.feple_backend.festival.setlistrequest.service;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.setlistrequest.repository.SetlistChangeRequestRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SetlistChangeRequestServiceTest {

    @Mock SetlistChangeRequestRepository repository;
    @Mock UserRepository userRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;

    @InjectMocks SetlistChangeRequestService service;

    @Test
    void submit_성공() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        ArtistFestival artistFestival = mock(ArtistFestival.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(festival));
        given(festival.getTitle()).willReturn("펜타포트");
        given(artistFestivalRepository.findById(100L)).willReturn(Optional.of(artistFestival));
        given(artistFestival.getFestivalId()).willReturn(10L);

        assertThatCode(() -> service.submit(1L, 10L, 100L, "아이유", "셋리스트 추가 요청"))
                .doesNotThrowAnyException();

        then(repository).should().save(any());
    }

    @Test
    void submit_존재하지_않는_참여정보면_예외() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(festival));
        given(artistFestivalRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(1L, 10L, 999L, "아이유", "메시지"))
                .isInstanceOf(java.util.NoSuchElementException.class);

        then(repository).shouldHaveNoInteractions();
    }

    @Test
    void submit_다른_페스티벌_참여정보면_예외() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        ArtistFestival artistFestival = mock(ArtistFestival.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(festival));
        given(artistFestivalRepository.findById(100L)).willReturn(Optional.of(artistFestival));
        given(artistFestival.getFestivalId()).willReturn(999L);

        assertThatThrownBy(() -> service.submit(1L, 10L, 100L, "아이유", "메시지"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 참여 정보가 아닙니다.");

        then(repository).shouldHaveNoInteractions();
    }
}
