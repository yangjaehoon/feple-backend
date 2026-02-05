package com.feple.feple_backend.file;

import com.feple.feple_backend.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posters")
public class PosterController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value="festivalStartDate", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate festivalStartDate
    ) throws IOException {
        return fileStorageService.storeFestivalPoster(file, festivalStartDate);
    }
}