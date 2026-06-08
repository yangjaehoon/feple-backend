package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestionStatus;
import com.feple.feple_backend.artist.suggestion.event.ArtistSuggestionProcessedEvent;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistSuggestionServiceImpl implements ArtistSuggestionService, ArtistSuggestionAdminService {

    private final ArtistSuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ArtistSuggestionResponseDto submit(Long userId, SubmitArtistSuggestionDto dto) {
        boolean alreadyRequested = suggestionRepository
                .existsByUserIdAndArtistNameIgnoreCaseAndStatus(
                        userId, dto.getArtistName(), ArtistSuggestionStatus.PENDING);
        if (alreadyRequested) {
            throw new ConflictException("이미 신청한 아티스트입니다.");
        }

        ArtistSuggestion suggestion = ArtistSuggestion.builder()
                .userId(userId)
                .artistName(dto.getArtistName())
                .note(dto.getNote())
                .build();

        ArtistSuggestion saved = suggestionRepository.save(suggestion);
        String nick = userRepository.findById(userId)
                .map(User::getNickname)
                .filter(n -> n != null && !n.isBlank())
                .orElse("알 수 없음");
        return ArtistSuggestionResponseDto.from(saved, nick);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistSuggestionResponseDto> getPendingSuggestions() {
        List<ArtistSuggestion> suggestions =
                suggestionRepository.findByStatusOrderByCreatedAtDesc(ArtistSuggestionStatus.PENDING);
        Map<Long, String> nMap = nicknameMap(suggestions);
        return suggestions.stream()
                .map(s -> ArtistSuggestionResponseDto.from(s, nickname(nMap, s.getUserId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArtistSuggestionResponseDto> getSuggestionsPage(int page, int size) {
        Page<ArtistSuggestion> pageResult = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                ArtistSuggestionStatus.PENDING, PageRequest.of(page, size));
        Map<Long, String> nMap = nicknameMap(pageResult.getContent());
        return pageResult.map(s -> ArtistSuggestionResponseDto.from(s, nickname(nMap, s.getUserId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistSuggestionResponseDto> getProcessedSuggestions() {
        List<ArtistSuggestion> suggestions =
                suggestionRepository.findByStatusOrderByCreatedAtDesc(ArtistSuggestionStatus.DISMISSED);
        Map<Long, String> nMap = nicknameMap(suggestions);
        return suggestions.stream()
                .map(s -> ArtistSuggestionResponseDto.from(s, nickname(nMap, s.getUserId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return suggestionRepository.countByStatus(ArtistSuggestionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistSuggestionResponseDto> getPendingSuggestionsPreview(int limit) {
        List<ArtistSuggestion> suggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                ArtistSuggestionStatus.PENDING, PageRequest.of(0, limit)).getContent();
        Map<Long, String> nMap = nicknameMap(suggestions);
        return suggestions.stream()
                .map(s -> ArtistSuggestionResponseDto.from(s, nickname(nMap, s.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public void dismiss(Long suggestionId, String processNote) {
        ArtistSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new NoSuchElementException("아티스트 신청을 찾을 수 없습니다: " + suggestionId));
        suggestion.dismiss(processNote);
        suggestionRepository.save(suggestion);
        eventPublisher.publishEvent(new ArtistSuggestionProcessedEvent(
                suggestion.getUserId(), suggestion.getArtistName(), processNote));
    }

    private Map<Long, String> nicknameMap(List<ArtistSuggestion> suggestions) {
        List<Long> ids = suggestions.stream().map(ArtistSuggestion::getUserId).distinct().toList();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> (u.getNickname() != null && !u.getNickname().isBlank())
                                ? u.getNickname() : "알 수 없음"
                ));
    }

    private String nickname(Map<Long, String> map, Long userId) {
        return map.getOrDefault(userId, "알 수 없음");
    }
}
