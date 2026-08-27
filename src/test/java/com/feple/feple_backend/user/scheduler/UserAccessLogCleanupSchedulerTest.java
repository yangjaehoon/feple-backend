package com.feple.feple_backend.user.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.feple.feple_backend.user.repository.UserAccessLogRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccessLogCleanupSchedulerTest {

    private static final int BATCH_SIZE = 1000;

    @Mock UserAccessLogRepository userAccessLogRepository;

    @InjectMocks UserAccessLogCleanupScheduler scheduler;

    @Test
    void 한_배치보다_적게_지워지면_한_번만_삭제한다() {
        given(userAccessLogRepository.deleteByAccessDateBeforeBatch(any(LocalDate.class), eq(BATCH_SIZE)))
                .willReturn(42);

        scheduler.cleanup();

        then(userAccessLogRepository).should()
                .deleteByAccessDateBeforeBatch(any(LocalDate.class), eq(BATCH_SIZE));
        then(userAccessLogRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void 배치가_가득_차면_빈_배치가_나올_때까지_반복_삭제한다() {
        given(userAccessLogRepository.deleteByAccessDateBeforeBatch(any(LocalDate.class), eq(BATCH_SIZE)))
                .willReturn(BATCH_SIZE, BATCH_SIZE, 137);

        scheduler.cleanup();

        then(userAccessLogRepository).should(times(3))
                .deleteByAccessDateBeforeBatch(any(LocalDate.class), eq(BATCH_SIZE));
    }

    @Test
    void 삭제_기준일은_실행일로부터_90일_이전이다() {
        given(userAccessLogRepository.deleteByAccessDateBeforeBatch(any(LocalDate.class), eq(BATCH_SIZE)))
                .willReturn(0);
        ArgumentCaptor<LocalDate> cutoffCaptor = ArgumentCaptor.forClass(LocalDate.class);

        scheduler.cleanup();

        then(userAccessLogRepository).should()
                .deleteByAccessDateBeforeBatch(cutoffCaptor.capture(), eq(BATCH_SIZE));
        assertThat(cutoffCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(90));
    }
}
