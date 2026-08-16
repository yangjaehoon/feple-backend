package com.feple.feple_backend.festival.setlistchangerequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequest;
import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequestStatus;
import com.feple.feple_backend.festival.setlistchangerequest.repository.SetlistChangeRequestRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SetlistChangeRequestServiceTest {

    @Mock SetlistChangeRequestRepository repository;
    @Mock UserRepository userRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock BadWordValidator badWordValidator;

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
        given(artistFestival.getArtistName()).willReturn("아이유");

        assertThatCode(() -> service.submit(new SetlistChangeRequestCommand(1L, 10L, 100L, "셋리스트 변경 요청")))
                .doesNotThrowAnyException();

        then(repository).should().save(any());
    }

    @Test
    void submit_금칙어가_포함되면_예외() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("금칙어가 포함되어 있습니다."))
                .when(badWordValidator).validate("금칙어 포함 메시지");

        assertThatThrownBy(() -> service.submit(new SetlistChangeRequestCommand(1L, 10L, 100L, "금칙어 포함 메시지")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금칙어가 포함되어 있습니다.");

        then(userRepository).shouldHaveNoInteractions();
        then(repository).should(never()).save(any());
    }

    @Test
    void submit_이미_대기중인_동일_요청이면_예외() {
        given(repository.existsByUserIdAndArtistFestivalIdAndStatus(1L, 100L, SetlistChangeRequestStatus.PENDING))
                .willReturn(true);

        assertThatThrownBy(() -> service.submit(new SetlistChangeRequestCommand(1L, 10L, 100L, "또 요청")))
                .isInstanceOf(com.feple.feple_backend.global.exception.ConflictException.class)
                .hasMessageContaining("이미 처리 대기 중인");

        then(userRepository).shouldHaveNoInteractions();
        then(repository).should(never()).save(any());
    }

    @Test
    void submit_존재하지_않는_참여정보면_예외() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(festival));
        given(artistFestivalRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(new SetlistChangeRequestCommand(1L, 10L, 999L, "메시지")))
                .isInstanceOf(java.util.NoSuchElementException.class);

        then(repository).should(never()).save(any());
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

        assertThatThrownBy(() -> service.submit(new SetlistChangeRequestCommand(1L, 10L, 100L, "메시지")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 참여 정보가 아닙니다.");

        then(repository).should(never()).save(any());
    }

    @Test
    void list_키워드_없으면_상태만으로_조회() {
        Page<SetlistChangeRequest> page = new PageImpl<>(java.util.List.of());
        given(repository.findByStatus(SetlistChangeRequestStatus.PENDING, PageRequest.of(0, 10))).willReturn(page);

        Page<SetlistChangeRequest> result = service.list(SetlistChangeRequestStatus.PENDING, null, PageRequest.of(0, 10));

        assertThat(result).isSameAs(page);
        then(repository).should(never()).findByStatusAndKeyword(any(), any(), any());
    }

    @Test
    void list_키워드_공백이면_상태만으로_조회() {
        Page<SetlistChangeRequest> page = new PageImpl<>(java.util.List.of());
        given(repository.findByStatus(SetlistChangeRequestStatus.PENDING, PageRequest.of(0, 10))).willReturn(page);

        Page<SetlistChangeRequest> result = service.list(SetlistChangeRequestStatus.PENDING, "  ", PageRequest.of(0, 10));

        assertThat(result).isSameAs(page);
    }

    @Test
    void list_키워드_있으면_키워드로_조회() {
        Page<SetlistChangeRequest> page = new PageImpl<>(java.util.List.of());
        given(repository.findByStatusAndKeyword(SetlistChangeRequestStatus.PENDING, "아이유", PageRequest.of(0, 10)))
                .willReturn(page);

        Page<SetlistChangeRequest> result = service.list(SetlistChangeRequestStatus.PENDING, "아이유", PageRequest.of(0, 10));

        assertThat(result).isSameAs(page);
        then(repository).should(never()).findByStatus(any(), any());
    }

    @Test
    void getPendingCount는_PENDING_상태_카운트_위임() {
        given(repository.countByStatus(SetlistChangeRequestStatus.PENDING)).willReturn(5L);

        assertThat(service.getPendingCount()).isEqualTo(5L);
    }

    @Test
    void countByStatus는_레포지토리에_위임() {
        given(repository.countByStatus(SetlistChangeRequestStatus.PROCESSED)).willReturn(3L);

        assertThat(service.countByStatus(SetlistChangeRequestStatus.PROCESSED)).isEqualTo(3L);
    }

    @Test
    void resolve_성공시_상태가_PROCESSED로_변경() {
        User user = mock(User.class);
        SetlistChangeRequest req = SetlistChangeRequest.of(user, 10L, 100L, "아이유", "펜타포트", "메시지");
        given(repository.findById(1L)).willReturn(Optional.of(req));

        service.resolve(1L);

        assertThat(req.getStatus()).isEqualTo(SetlistChangeRequestStatus.PROCESSED);
    }

    @Test
    void resolve_존재하지_않으면_예외() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(99L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
