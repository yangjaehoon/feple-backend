package com.feple.feple_backend.admin.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AdminLogFilterTest {

    @Test
    void null_문자열_필드는_빈_문자열로_정규화() {
        AdminLogFilter filter = new AdminLogFilter(null, null, null, null, null);

        assertThat(filter.targetType()).isEmpty();
        assertThat(filter.adminUsername()).isEmpty();
        assertThat(filter.page()).isZero();
    }

    @Test
    void null_음수_page는_0으로_보정() {
        assertThat(new AdminLogFilter("", "", null, null, null).page()).isZero();
        assertThat(new AdminLogFilter("", "", null, null, -5).page()).isZero();
        assertThat(new AdminLogFilter("", "", null, null, 3).page()).isEqualTo(3);
    }

    @Test
    void 날짜_필드는_그대로_유지() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        AdminLogFilter filter = new AdminLogFilter("POST", "admin", from, from.plusDays(30), 0);

        assertThat(filter.from()).isEqualTo(from);
        assertThat(filter.to()).isEqualTo(from.plusDays(30));
    }
}
