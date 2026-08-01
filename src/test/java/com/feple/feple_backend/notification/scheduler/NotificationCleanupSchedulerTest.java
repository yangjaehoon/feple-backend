package com.feple.feple_backend.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.feple.feple_backend.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

    private static final int BATCH_SIZE = 1000;

    @Mock NotificationRepository notificationRepository;

    @InjectMocks NotificationCleanupScheduler scheduler;

    @Test
    void 정리_90일_이전_알림_배치삭제_요청() {
        scheduler.cleanup();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(notificationRepository).should().deleteOlderThanBatch(captor.capture(), eq(BATCH_SIZE));

        LocalDateTime cutoff = captor.getValue();
        assertThat(cutoff).isBefore(LocalDateTime.now().minusDays(89));
        assertThat(cutoff).isAfter(LocalDateTime.now().minusDays(91));
    }

    @Test
    void 정리_삭제건수가_배치크기와_같으면_다음_배치_반복호출() {
        given(notificationRepository.deleteOlderThanBatch(any(), eq(BATCH_SIZE)))
                .willReturn(BATCH_SIZE, 200);

        scheduler.cleanup();

        then(notificationRepository).should(times(2)).deleteOlderThanBatch(any(), eq(BATCH_SIZE));
    }
}
