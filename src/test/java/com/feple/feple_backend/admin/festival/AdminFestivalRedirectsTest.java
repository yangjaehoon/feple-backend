package com.feple.feple_backend.admin.festival;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminFestivalRedirectsTest {

    @Test
    void detail_페스티벌_상세_리다이렉트() {
        assertThat(AdminFestivalRedirects.detail(1L)).isEqualTo("redirect:/admin/festivals/1");
    }

    @Test
    void artists_아티스트_탭_앵커_포함() {
        assertThat(AdminFestivalRedirects.artists(1L)).isEqualTo("redirect:/admin/festivals/1#artists");
    }

    @Test
    void timetable_타임테이블_탭_앵커_포함() {
        assertThat(AdminFestivalRedirects.timetable(1L)).isEqualTo("redirect:/admin/festivals/1#timetable");
    }

    @Test
    void booths_부스_탭_앵커_포함() {
        assertThat(AdminFestivalRedirects.booths(1L)).isEqualTo("redirect:/admin/festivals/1#booths");
    }

    @Test
    void setlist_셋리스트_탭_앵커_포함() {
        assertThat(AdminFestivalRedirects.setlist(1L)).isEqualTo("redirect:/admin/festivals/1#setlist");
    }
}
