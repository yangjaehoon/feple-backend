package com.feple.feple_backend.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.feple.feple_backend.diary.dto.CreateDiaryRequestDto;
import com.feple.feple_backend.diary.dto.FestivalDiaryResponseDto;
import com.feple.feple_backend.diary.dto.UpdateDiaryRequestDto;
import com.feple.feple_backend.diary.entity.DiaryVisibility;
import com.feple.feple_backend.diary.service.FestivalDiaryService;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.support.AuthTestHelper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FestivalDiaryControllerTest {

    @Mock FestivalDiaryService diaryService;

    @InjectMocks FestivalDiaryController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 컨트롤러가 Page<T>를 직접 반환하므로 PageImpl 직렬화를 위한 PageModule을 등록한다.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SpringDataWebSettings settings = new SpringDataWebSettings(
                EnableSpringDataWebSupport.PageSerializationMode.DIRECT);
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(settings));
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    private FestivalDiaryResponseDto sampleDiary(long id) {
        return new FestivalDiaryResponseDto(
                id, 100L, "락페스티벌", "Rock Festival", "오늘 최고였다",
                DiaryVisibility.PUBLIC, List.of("https://cdn/1.jpg"),
                null, null, true, "닉네임");
    }

    // ── POST /diaries/presign ────────────────────────────────────────────

    @Test
    void presign_허용된_확장자면_업로드_URL을_반환한다() throws Exception {
        given(diaryService.generateUploadUrl(1L, "jpg", "image/jpeg"))
                .willReturn(new S3PresignedUrlResult("https://s3/put", "diary/1/abc.jpg"));

        mockMvc.perform(post("/diaries/presign")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"extension\":\"jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey").value("diary/1/abc.jpg"));
    }

    @Test
    void presign_확장자와_MIME이_불일치하면_400() throws Exception {
        mockMvc.perform(post("/diaries/presign")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/png\",\"extension\":\"jpg\"}"))
                .andExpect(status().isBadRequest());

        then(diaryService).shouldHaveNoInteractions();
    }

    // ── POST /diaries ───────────────────────────────────────────────────

    @Test
    void 일기_생성_성공시_201과_생성된_일기를_반환한다() throws Exception {
        given(diaryService.create(eq(1L), eq(100L), any(CreateDiaryRequestDto.class)))
                .willReturn(sampleDiary(7L));

        mockMvc.perform(post("/diaries")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"festivalId\":100,\"content\":\"오늘 최고였다\",\"visibility\":\"PUBLIC\",\"photoKeys\":[\"k1\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void 일기_생성_내용이_비면_400() throws Exception {
        mockMvc.perform(post("/diaries")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"festivalId\":100,\"content\":\"\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /diaries/mine ──────────────────────────────────────────────

    @Test
    void 내_일기_목록_festivalId_없이_조회() throws Exception {
        given(diaryService.getMyDiaries(1L, null)).willReturn(List.of(sampleDiary(1L)));

        mockMvc.perform(get("/diaries/mine").with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 내_일기_목록_festivalId로_필터링() throws Exception {
        given(diaryService.getMyDiaries(1L, 100L)).willReturn(List.of());

        mockMvc.perform(get("/diaries/mine").param("festivalId", "100")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());

        then(diaryService).should().getMyDiaries(1L, 100L);
    }

    // ── GET /diaries/{id} ──────────────────────────────────────────────

    @Test
    void 일기_단건_조회_인증없으면_viewerId는_null로_전달() throws Exception {
        given(diaryService.getDiary(isNull(), eq(5L))).willReturn(sampleDiary(5L));

        mockMvc.perform(get("/diaries/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void 일기_단건_조회_인증되면_viewerId_전달() throws Exception {
        given(diaryService.getDiary(9L, 5L)).willReturn(sampleDiary(5L));

        mockMvc.perform(get("/diaries/5").with(AuthTestHelper.userAuth(9L)))
                .andExpect(status().isOk());

        then(diaryService).should().getDiary(9L, 5L);
    }

    // ── PUT /diaries/{id} ──────────────────────────────────────────────

    @Test
    void 일기_수정_성공() throws Exception {
        given(diaryService.update(eq(1L), eq(5L), any(UpdateDiaryRequestDto.class)))
                .willReturn(sampleDiary(5L));

        mockMvc.perform(put("/diaries/5")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정됨\",\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isOk());
    }

    // ── DELETE /diaries/{id} ───────────────────────────────────────────

    @Test
    void 일기_삭제_성공시_204() throws Exception {
        mockMvc.perform(delete("/diaries/5").with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());

        then(diaryService).should().delete(1L, 5L);
    }

    // ── GET /diaries/festival/{festivalId}/public ──────────────────────

    @Test
    void 페스티벌_공개_피드_기본_페이지는_0() throws Exception {
        Page<FestivalDiaryResponseDto> page = new PageImpl<>(List.of(sampleDiary(1L)));
        given(diaryService.getPublicFeed(eq(100L), eq(0), isNull())).willReturn(page);

        mockMvc.perform(get("/diaries/festival/100/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void 페스티벌_공개_피드_page_파라미터와_viewerId_전달() throws Exception {
        given(diaryService.getPublicFeed(100L, 2, 9L)).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/diaries/festival/100/public").param("page", "2")
                        .with(AuthTestHelper.userAuth(9L)))
                .andExpect(status().isOk());

        then(diaryService).should().getPublicFeed(100L, 2, 9L);
    }

    // ── GET /diaries/user/{userId}/public ─────────────────────────────

    @Test
    void 특정_유저_공개_일기_조회() throws Exception {
        given(diaryService.getUserPublicDiaries(eq(42L), eq(0), isNull()))
                .willReturn(new PageImpl<>(List.of(sampleDiary(1L))));

        mockMvc.perform(get("/diaries/user/42/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
