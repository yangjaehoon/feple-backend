package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestionStatus;
import com.feple.feple_backend.artist.suggestion.event.ArtistSuggestionProcessedEvent;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.UserNicknameLookup;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// FestivalSuggestionServiceImpl과 엔티티·리포지토리·서비스 구조가 거의 동일하지만, 두 엔티티가
// 서로 다른 테이블(artistName/approvedArtistId ↔ festivalName/approvedFestivalId)이라 제네릭
// 서비스로 묶으려면 필드 접근용 콜백이 늘어나 오히려 읽기 어려워지고, 테이블을 합치려면 운영 공유
// DB에 스키마 변경이 필요해 리스크가 커진다. ArtistPhotoReportService와 같은 이유로 통합하지 않는다.
@Service
@RequiredArgsConstructor
public class ArtistSuggestionServiceImpl implements ArtistSuggestionService, ArtistSuggestionAdminService {

    private final ArtistSuggestionRepository suggestionRepository;
    private final UserNicknameLookup nicknameResolver;
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
        return ArtistSuggestionResponseDto.from(saved, nicknameResolver.lookup(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArtistSuggestionResponseDto> getSuggestionsPage(int page, int size) {
        Page<ArtistSuggestion> pageResult = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                ArtistSuggestionStatus.PENDING, PageRequest.of(page, size));
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(pageResult.getContent(), ArtistSuggestion::getUserId);
        return pageResult.map(s -> ArtistSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameLookup.UNKNOWN)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistSuggestionResponseDto> getProcessedSuggestionsPreview(int limit) {
        List<ArtistSuggestion> suggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                ArtistSuggestionStatus.DISMISSED, PageRequest.of(0, limit)).getContent();
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(suggestions, ArtistSuggestion::getUserId);
        return suggestions.stream()
                .map(s -> ArtistSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameLookup.UNKNOWN)))
                .toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'suggestionCount'")
    @Transactional(readOnly = true)
    public long getPendingCount() {
        return suggestionRepository.countByStatus(ArtistSuggestionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long getProcessedCount() {
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
                .map(s -> ArtistSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameLookup.UNKNOWN)))
                .toList();
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void approve(Long suggestionId, Long artistId) {
        ArtistSuggestion suggestion = EntityLoader.getOrThrow(suggestionRepository::findById, suggestionId, "아티스트 신청");
        requirePending(suggestion);
        suggestion.approve(artistId);
        eventPublisher.publishEvent(new ArtistSuggestionProcessedEvent(
                suggestion.getUserId(), artistId, suggestion.getArtistName(), null));
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void dismiss(Long suggestionId, String processNote) {
        ArtistSuggestion suggestion = EntityLoader.getOrThrow(suggestionRepository::findById, suggestionId, "아티스트 신청");
        requirePending(suggestion);
        suggestion.dismiss(processNote);
        eventPublisher.publishEvent(new ArtistSuggestionProcessedEvent(
                suggestion.getUserId(), null, suggestion.getArtistName(), processNote));
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        suggestionRepository.deleteByUserId(userId);
    }

    // 이중 클릭·요청 재시도로 동일 신청이 두 번 승인/반려되며 알림이 중복 발송되는 것을 방지
    private void requirePending(ArtistSuggestion suggestion) {
        if (!suggestion.isPending()) {
            throw new InvalidRequestException("이미 처리된 아티스트 신청입니다.");
        }
    }
}
