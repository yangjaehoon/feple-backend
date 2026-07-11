package com.feple.feple_backend.artist;

import com.feple.feple_backend.artist.repository.ArtistRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ArtistNameValidator {

    private final ArtistRepository artistRepository;
    private volatile Set<String> artistNames = Set.of();

    @PostConstruct
    public void reload() {
        Set<String> names = new HashSet<>();
        artistRepository.findAllKoreanNames().stream()
                .map(n -> n.toLowerCase().replaceAll("\\s+", ""))
                .filter(n -> n.length() >= 2)
                .forEach(names::add);
        artistRepository.findAllEnglishNames().stream()
                .map(n -> n.toLowerCase().replaceAll("\\s+", ""))
                .filter(n -> n.length() >= 2)
                .forEach(names::add);
        this.artistNames = Set.copyOf(names);
    }

    public void validate(String nickname) {
        if (nickname == null) return;
        Set<String> snapshot = artistNames;
        if (snapshot.isEmpty()) return;
        String normalized = nickname.toLowerCase().replaceAll("\\s+", "");
        for (String artistName : snapshot) {
            if (normalized.contains(artistName)) {
                throw new IllegalArgumentException("아티스트 이름은 닉네임으로 사용할 수 없습니다.");
            }
        }
    }
}
