package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestionStatus;
import com.feple.feple_backend.artist.suggestion.event.ArtistSuggestionProcessedEvent;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.global.UserNicknameResolver;
import com.feple.feple_backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.feple.feple_backend.global.EntityFinder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArtistSuggestionServiceImpl implements ArtistSuggestionService, ArtistSuggestionAdminService {

    private final ArtistSuggestionRepository suggestionRepository;
    private final UserNicknameResolver nicknameResolver;
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
        return ArtistSuggestionResponseDto.from(saved, nicknameResolver.resolve(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArtistSuggestionResponseDto> getSuggestionsPage(int page, int size) {
        Page<ArtistSuggestion> pageResult = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                ArtistSuggestionStatus.PENDING, PageRequest.of(page, size));
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(pageResult.getContent(), ArtistSuggestion::getUserId);
        return pageResult.map(s -> ArtistSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameResolver.UNKNOWN)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistSuggestionResponseDto> getProcessedSuggestionsPreview(int limit) {
        List<ArtistSuggestion> suggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                ArtistSuggestionStatus.DISMISSED, PageRequest.of(0, limit)).getContent();
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(suggestions, ArtistSuggestion::getUserId);
        return suggestions.stream()
                .map(s -> ArtistSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameResolver.UNKNOWN)))
                .toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'suggestionCount'")
    @Transactional(readOnly = true)
    public long countPending() {
        return suggestionRepository.countByStatus(ArtistSuggestionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProcessed() {
        return suggestionRepository.countByStatus(ArtistSuggestionStatus.DISMISSED);
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'suggestions_' + #limit")
    @Transactional(readOnly = true)
    public List<ArtistSuggestionResponseDto> getPendingSuggestionsPreview(int limit) {
        List<ArtistSuggestion> suggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                ArtistSuggestionStatus.PENDING, PageRequest.of(0, limit)).getContent();
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(suggestions, ArtistSuggestion::getUserId);
        return suggestions.stream()
                .map(s -> ArtistSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameResolver.UNKNOWN)))
                .toList();
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void dismiss(Long suggestionId, String processNote) {
        ArtistSuggestion suggestion = EntityFinder.getOrThrow(suggestionRepository::findById, suggestionId, "아티스트 신청");
        suggestion.dismiss(processNote);
        eventPublisher.publishEvent(new ArtistSuggestionProcessedEvent(
                suggestion.getUserId(), suggestion.getArtistName(), processNote));
    }

}
