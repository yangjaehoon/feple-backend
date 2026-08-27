package com.feple.feple_backend.notice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import com.feple.feple_backend.notice.dto.NoticeSummaryDto;
import com.feple.feple_backend.notice.service.NoticeService;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NoticeControllerTest {

    @Mock NoticeService noticeService;

    @InjectMocks NoticeController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SpringDataWebSettings settings = new SpringDataWebSettings(
                EnableSpringDataWebSupport.PageSerializationMode.DIRECT);
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(settings));
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void 공지_목록_기본_페이지_파라미터로_조회한다() throws Exception {
        NoticeSummaryDto dto = NoticeSummaryDto.builder().id(1L).title("점검 안내").pinned(true).build();
        given(noticeService.getNotices(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("점검 안내"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        then(noticeService).should().getNotices(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void 공지_목록_page_size_파라미터가_전달된다() throws Exception {
        given(noticeService.getNotices(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/notices").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        then(noticeService).should().getNotices(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void 공지_단건_조회_성공() throws Exception {
        NoticeResponseDto dto = NoticeResponseDto.builder()
                .id(9L).title("제목").content("본문").pinned(false).build();
        given(noticeService.getNotice(9L)).willReturn(dto);

        mockMvc.perform(get("/notices/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.content").value("본문"));
    }

    @Test
    void 공지_단건_조회_없으면_404() throws Exception {
        given(noticeService.getNotice(99L))
                .willThrow(new NoSuchElementException("공지사항을(를) 찾을 수 없습니다: 99"));

        mockMvc.perform(get("/notices/99"))
                .andExpect(status().isNotFound());
    }
}
