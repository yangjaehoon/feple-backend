package com.feple.feple_backend.notification.scheduler;

import com.feple.feple_backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

    @Mock NotificationRepository notificationRepository;

    @InjectMocks NotificationCleanupScheduler scheduler;

    @Test
    void 정리_90일_이전_알림_삭제_요청() {
        scheduler.cleanup();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(notificationRepository).should().deleteOlderThan(captor.capture());

        LocalDateTime cutoff = captor.getValue();
        assertThat(cutoff).isBefore(LocalDateTime.now().minusDays(89));
        assertThat(cutoff).isAfter(LocalDateTime.now().minusDays(91));
    }
}
