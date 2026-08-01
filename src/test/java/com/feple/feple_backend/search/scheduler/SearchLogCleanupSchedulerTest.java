package com.feple.feple_backend.search.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.feple.feple_backend.search.repository.SearchLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchLogCleanupSchedulerTest {

    private static final int BATCH_SIZE = 1000;

    @Mock SearchLogRepository searchLogRepository;

    @InjectMocks SearchLogCleanupScheduler scheduler;

    @Test
    void 정리_90일_이전_로그_배치삭제_요청() {
        scheduler.cleanup();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(searchLogRepository).should().deleteByCreatedAtBeforeBatch(captor.capture(), eq(BATCH_SIZE));

        LocalDateTime cutoff = captor.getValue();
        assertThat(cutoff).isBefore(LocalDateTime.now().minusDays(89));
        assertThat(cutoff).isAfter(LocalDateTime.now().minusDays(91));
    }

    @Test
    void 정리_삭제건수가_배치크기와_같으면_다음_배치_반복호출() {
        given(searchLogRepository.deleteByCreatedAtBeforeBatch(any(), eq(BATCH_SIZE)))
                .willReturn(BATCH_SIZE, 300);

        scheduler.cleanup();

        then(searchLogRepository).should(times(2)).deleteByCreatedAtBeforeBatch(any(), eq(BATCH_SIZE));
    }
}
