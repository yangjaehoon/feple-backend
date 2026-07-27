package com.feple.feple_backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReportCommandServiceTest {

    private final ReportCommandService service = mock(ReportCommandService.class, CALLS_REAL_METHODS);

    @Test
    void 전부_성공하면_전체_개수를_반환한다() {
        int result = service.bulkDeleteContent(List.of(1L, 2L, 3L));

        assertThat(result).isEqualTo(3);
        verify(service, times(3)).deleteContentAndResolve(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void 일부_실패해도_나머지는_계속_처리하고_성공_개수만_반환한다() {
        willThrow(new RuntimeException("실패")).given(service).deleteContentAndResolve(2L);

        int result = service.bulkDeleteContent(List.of(1L, 2L, 3L));

        assertThat(result).isEqualTo(2);
        verify(service).deleteContentAndResolve(1L);
        verify(service).deleteContentAndResolve(2L);
        verify(service).deleteContentAndResolve(3L);
    }
}
