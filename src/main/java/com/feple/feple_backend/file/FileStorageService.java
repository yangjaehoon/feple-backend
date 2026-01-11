package com.feple.feple_backend.file;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) throws IOException {
        if(file.isEmpty()){
            throw new IllegalArgumentException("File is empty");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if(originalFilename != null && originalFilename.contains(".")){
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedName = UUID.randomUUID() + ext;

        Path target = uploadPath.resolve(savedName);
        file.transferTo(target.toFile());

        return "/posters" + savedName;
    }
}
