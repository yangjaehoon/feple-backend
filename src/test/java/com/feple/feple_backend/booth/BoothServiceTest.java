package com.feple.feple_backend.booth;

import com.feple.feple_backend.booth.dto.BoothRequestDto;
import com.feple.feple_backend.booth.entity.Booth;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.booth.repository.BoothRepository;
import com.feple.feple_backend.booth.service.BoothService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BoothServiceTest {

    @Mock BoothRepository boothRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks BoothService boothService;

    @Test
    void createBooth_페스티벌_없으면_예외() {
        given(festivalRepository.findById(1L)).willReturn(Optional.empty());

        BoothRequestDto dto = BoothRequestDto.builder()
                .name("음식 부스")
                .boothType(BoothType.FOOD)
                .latitude(37.5)
                .longitude(127.0)
                .build();

        assertThatThrownBy(() -> boothService.createBooth(1L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createBooth_성공_부스_ID_반환() {
        Festival festival = mock(Festival.class);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(festival));

        Booth savedBooth = mock(Booth.class);
        given(savedBooth.getId()).willReturn(5L);
        given(boothRepository.save(any(Booth.class))).willReturn(savedBooth);

        BoothRequestDto dto = BoothRequestDto.builder()
                .name("음식 부스")
                .boothType(BoothType.FOOD)
                .latitude(37.5)
                .longitude(127.0)
                .build();

        Long result = boothService.createBooth(1L, dto);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void deleteBooth_부스_없으면_예외() {
        given(boothRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boothService.deleteBooth(1L, 10L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteBooth_다른_페스티벌_예외() {
        Booth booth = mock(Booth.class);
        given(booth.getFestivalId()).willReturn(99L);
        given(boothRepository.findById(10L)).willReturn(Optional.of(booth));

        assertThatThrownBy(() -> boothService.deleteBooth(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 부스가 아닙니다.");
    }

    @Test
    void deleteBooth_성공() {
        Booth booth = mock(Booth.class);
        given(booth.getFestivalId()).willReturn(1L);
        given(booth.getImageKey()).willReturn("booths/img.jpg");
        given(boothRepository.findById(10L)).willReturn(Optional.of(booth));

        boothService.deleteBooth(1L, 10L);

        then(boothRepository).should().delete(booth);
        then(fileStorageService).should().deleteFileAfterCommit("booths/img.jpg");
    }

    @Test
    void 페스티벌_전체삭제시_부스_이미지도_S3에서_정리() {
        Booth booth1 = mock(Booth.class);
        given(booth1.getImageKey()).willReturn("booths/a.jpg");
        Booth booth2 = mock(Booth.class);
        given(booth2.getImageKey()).willReturn("booths/b.jpg");
        given(boothRepository.findByFestivalId(1L)).willReturn(java.util.List.of(booth1, booth2));

        boothService.removeAllByFestival(1L);

        then(fileStorageService).should().deleteFileAfterCommit("booths/a.jpg");
        then(fileStorageService).should().deleteFileAfterCommit("booths/b.jpg");
        then(boothRepository).should().deleteByFestivalId(1L);
    }
}
