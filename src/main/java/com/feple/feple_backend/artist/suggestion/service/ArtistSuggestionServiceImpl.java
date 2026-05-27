package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestionStatus;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ArtistSuggestionServiceImpl implements ArtistSuggestionService, ArtistSuggestionAdminService {

    private final ArtistSuggestionRepository suggestionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ArtistSuggestionResponseDto submit(Long userId, SubmitArtistSuggestionDto dto) {
        boolean alreadyRequested = suggestionRepository
                .existsByUserIdAndArtistNameIgnoreCaseAndStatus(
                        userId, dto.getArtistName(), ArtistSuggestionStatus.PENDING);
        if (alreadyRequested) {
            throw new IllegalArgumentException("이미 신청한 아티스트입니다.");
        }

        ArtistSuggestion suggestion = ArtistSuggestion.builder()
                .userId(userId)
                .artistName(dto.getArtistName())
                .note(dto.getNote())
                .build();

        ArtistSuggestion saved = suggestionRepository.save(suggestion);
        return ArtistSuggestionResponseDto.from(saved, resolveNickname(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistSuggestionResponseDto> getPendingSuggestions() {
        return suggestionRepository.findByStatusOrderByCreatedAtDesc(ArtistSuggestionStatus.PENDING)
                .stream()
                .map(s -> ArtistSuggestionResponseDto.from(s, resolveNickname(s.getUserId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return suggestionRepository.countByStatus(ArtistSuggestionStatus.PENDING);
    }

    @Override
    @Transactional
    public void dismiss(Long suggestionId) {
        ArtistSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new NoSuchElementException("아티스트 신청을 찾을 수 없습니다: " + suggestionId));
        suggestion.dismiss();
        suggestionRepository.save(suggestion);
    }

    private String resolveNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .filter(n -> n != null && !n.isBlank())
                .orElse("알 수 없음");
    }
}
