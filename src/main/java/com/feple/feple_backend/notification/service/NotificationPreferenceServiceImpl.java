package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.notification.dto.NotificationPreferenceDto;
import com.feple.feple_backend.notification.dto.UpdateNotificationPreferenceDto;
import com.feple.feple_backend.notification.entity.NotificationPreference;
import com.feple.feple_backend.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    @Transactional
    public NotificationPreferenceDto getPreferences(Long userId) {
        return NotificationPreferenceDto.from(getOrCreate(userId));
    }

    @Override
    @Transactional
    public NotificationPreferenceDto updatePreferences(Long userId, UpdateNotificationPreferenceDto dto) {
        NotificationPreference pref = getOrCreate(userId);
        pref.update(dto.isCertEnabled(), dto.isCommentEnabled(),
                dto.isFestivalEnabled(), dto.isSongRequestEnabled());
        return NotificationPreferenceDto.from(preferenceRepository.save(pref));
    }

    @Override
    @Transactional
    public NotificationPreference getOrCreate(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return preferenceRepository.save(
                                NotificationPreference.defaultFor(userId));
                    } catch (DataIntegrityViolationException e) {
                        // 동시 요청으로 다른 트랜잭션이 먼저 insert한 경우
                        return preferenceRepository.findByUserId(userId)
                                .orElseThrow(() -> e);
                    }
                });
    }
}
