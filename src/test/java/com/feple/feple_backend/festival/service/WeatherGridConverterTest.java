package com.feple.feple_backend.festival.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WeatherGridConverterTest {

    @Test
    void 서울시청_좌표는_기상청_서울_격자와_일치() {
        int[] grid = WeatherGridConverter.toGrid(37.5665, 126.9780);

        assertThat(grid).containsExactly(60, 127);
    }

    @Test
    void 부산시청_좌표는_기상청_부산_격자와_일치() {
        int[] grid = WeatherGridConverter.toGrid(35.1796, 129.0756);

        assertThat(grid).containsExactly(98, 76);
    }

    @Test
    void 인천시청_좌표는_기상청_인천_격자와_일치() {
        int[] grid = WeatherGridConverter.toGrid(37.4563, 126.7052);

        assertThat(grid).containsExactly(55, 124);
    }
}
