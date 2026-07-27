package com.feple.feple_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.notification.dto.NotificationPreferenceDto;
import com.feple.feple_backend.notification.dto.UpdateNotificationPreferenceDto;
import com.feple.feple_backend.notification.entity.NotificationPreference;
import com.feple.feple_backend.notification.repository.NotificationPreferenceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock NotificationPreferenceRepository preferenceRepository;

    @InjectMocks NotificationPreferenceServiceImpl service;

    // ── getPreferences ───────────────────────────────────────────────────

    @Test
    void getPreferences_기존_설정이_있으면_그대로_반환() {
        NotificationPreference pref = NotificationPreference.defaultFor(1L);
        given(preferenceRepository.findByUserId(1L)).willReturn(Optional.of(pref));

        NotificationPreferenceDto result = service.getPreferences(1L);

        assertThat(result.isCertEnabled()).isTrue();
    }

    // ── updatePreferences ────────────────────────────────────────────────

    @Test
    void updatePreferences_기존_설정값을_갱신() {
        NotificationPreference pref = NotificationPreference.defaultFor(1L);
        given(preferenceRepository.findByUserId(1L)).willReturn(Optional.of(pref));
        UpdateNotificationPreferenceDto dto = mock(UpdateNotificationPreferenceDto.class);
        given(dto.isCertEnabled()).willReturn(false);
        given(dto.isCommentEnabled()).willReturn(false);
        given(dto.isFestivalEnabled()).willReturn(true);
        given(dto.isSongRequestEnabled()).willReturn(false);

        service.updatePreferences(1L, dto);

        assertThat(pref.isCertEnabled()).isFalse();
        assertThat(pref.isCommentEnabled()).isFalse();
        assertThat(pref.isFestivalEnabled()).isTrue();
        assertThat(pref.isSongRequestEnabled()).isFalse();
    }

    // ── getOrCreate ──────────────────────────────────────────────────────

    @Test
    void getOrCreate_존재하면_그대로_반환() {
        NotificationPreference pref = NotificationPreference.defaultFor(1L);
        given(preferenceRepository.findByUserId(1L)).willReturn(Optional.of(pref));

        NotificationPreference result = service.getOrCreate(1L);

        assertThat(result).isSameAs(pref);
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void getOrCreate_없으면_생성후_저장() {
        given(preferenceRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(preferenceRepository.save(any(NotificationPreference.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = service.getOrCreate(1L);

        assertThat(result.getUserId()).isEqualTo(1L);
    }

    @Test
    void getOrCreate_동시생성_경합시_재조회로_기존값_반환() {
        NotificationPreference existing = NotificationPreference.defaultFor(1L);
        given(preferenceRepository.findByUserId(1L))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existing));
        given(preferenceRepository.save(any(NotificationPreference.class)))
                .willThrow(new DataIntegrityViolationException("dup"));

        NotificationPreference result = service.getOrCreate(1L);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void getOrCreate_동시생성_경합후_재조회도_실패하면_예외전파() {
        given(preferenceRepository.findByUserId(1L))
                .willReturn(Optional.empty())
                .willReturn(Optional.empty());
        given(preferenceRepository.save(any(NotificationPreference.class)))
                .willThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.getOrCreate(1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── getOrCreateBatch ─────────────────────────────────────────────────

    @Test
    void getOrCreateBatch_기존_유저와_신규_유저_혼합() {
        NotificationPreference existing = NotificationPreference.defaultFor(1L);
        given(preferenceRepository.findAllByUserIdIn(List.of(1L, 2L))).willReturn(List.of(existing));
        given(preferenceRepository.saveAll(any()))
                .willAnswer(invocation -> List.of(NotificationPreference.defaultFor(2L)));

        Map<Long, NotificationPreference> result = service.getOrCreateBatch(List.of(1L, 2L));

        assertThat(result).containsKeys(1L, 2L);
    }

    @Test
    void getOrCreateBatch_전부_기존이면_saveAll_호출안함() {
        NotificationPreference existing = NotificationPreference.defaultFor(1L);
        given(preferenceRepository.findAllByUserIdIn(List.of(1L))).willReturn(List.of(existing));

        Map<Long, NotificationPreference> result = service.getOrCreateBatch(List.of(1L));

        assertThat(result).containsKey(1L);
        verify(preferenceRepository, never()).saveAll(any());
    }

    // ── removeAllByUser ──────────────────────────────────────────────────

    @Test
    void removeAllByUser_레포지토리에_위임() {
        service.removeAllByUser(1L);

        verify(preferenceRepository).deleteByUserId(1L);
    }
}
