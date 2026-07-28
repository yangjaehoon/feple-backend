package com.feple.feple_backend.global;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageableFactoryTest {

    @Test
    void orderByLatestFirst_createdAt_내림차순() {
        Pageable pageable = PageableFactory.orderByLatestFirst(0, 20);

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void orderByLatestId_id_내림차순() {
        Pageable pageable = PageableFactory.orderByLatestId(1, 10);

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
        assertThat(pageable.getPageNumber()).isEqualTo(1);
    }

    @Test
    void orderByLatestStartDate_startDate_내림차순() {
        Pageable pageable = PageableFactory.orderByLatestStartDate(2, 5);

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "startDate"));
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }
}
