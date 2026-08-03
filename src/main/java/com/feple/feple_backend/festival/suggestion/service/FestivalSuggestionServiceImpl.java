package com.feple.feple_backend.festival.suggestion.service;

import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import com.feple.feple_backend.festival.suggestion.dto.SubmitFestivalSuggestionDto;
import com.feple.feple_backend.festival.suggestion.entity.FestivalSuggestion;
import com.feple.feple_backend.festival.suggestion.entity.FestivalSuggestionStatus;
import com.feple.feple_backend.festival.suggestion.event.FestivalSuggestionProcessedEvent;
import com.feple.feple_backend.festival.suggestion.repository.FestivalSuggestionRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.UserNicknameLookup;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import com.feple.feple_backend.global.exception.ConflictException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ArtistSuggestionServiceImpl과 엔티티·리포지토리·서비스 구조가 거의 동일하지만, 두 엔티티가
// 서로 다른 테이블(festivalName/approvedFestivalId ↔ artistName/approvedArtistId)이라 제네릭
// 서비스로 묶으려면 필드 접근용 콜백이 늘어나 오히려 읽기 어려워지고, 테이블을 합치려면 운영 공유
// DB에 스키마 변경이 필요해 리스크가 커진다. ArtistPhotoReportService와 같은 이유로 통합하지 않는다.
@Service
@RequiredArgsConstructor
public class FestivalSuggestionServiceImpl implements FestivalSuggestionService, FestivalSuggestionAdminService {

    private final FestivalSuggestionRepository suggestionRepository;
    private final UserNicknameLookup nicknameResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public FestivalSuggestionResponseDto submit(Long userId, SubmitFestivalSuggestionDto dto) {
        boolean alreadyRequested = suggestionRepository
                .existsByUserIdAndFestivalNameIgnoreCaseAndStatus(
                        userId, dto.getFestivalName(), FestivalSuggestionStatus.PENDING);
        if (alreadyRequested) {
            throw new ConflictException("이미 신청한 페스티벌입니다.");
        }

        FestivalSuggestion suggestion = FestivalSuggestion.builder()
                .userId(userId)
                .festivalName(dto.getFestivalName())
                .note(dto.getNote())
                .build();

        FestivalSuggestion saved = suggestionRepository.save(suggestion);
        return FestivalSuggestionResponseDto.from(saved, nicknameResolver.lookup(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalSuggestionResponseDto> getSuggestionsPage(int page, int size) {
        Page<FestivalSuggestion> pageResult = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                FestivalSuggestionStatus.PENDING, PageRequest.of(page, size));
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(pageResult.getContent(), FestivalSuggestion::getUserId);
        return pageResult.map(s -> FestivalSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameLookup.UNKNOWN)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalSuggestionResponseDto> getProcessedSuggestionsPreview(int limit) {
        List<FestivalSuggestion> suggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                FestivalSuggestionStatus.DISMISSED, PageRequest.of(0, limit)).getContent();
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(suggestions, FestivalSuggestion::getUserId);
        return suggestions.stream()
                .map(s -> FestivalSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameLookup.UNKNOWN)))
                .toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'festivalSuggestionCount'")
    @Transactional(readOnly = true)
    public long getPendingCount() {
        return suggestionRepository.countByStatus(FestivalSuggestionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long getProcessedCount() {
        return suggestionRepository.countByStatus(FestivalSuggestionStatus.DISMISSED);
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'festivalSuggestions_' + #limit")
    @Transactional(readOnly = true)
    public List<FestivalSuggestionResponseDto> getPendingSuggestionsPreview(int limit) {
        List<FestivalSuggestion> suggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc(
                FestivalSuggestionStatus.PENDING, PageRequest.of(0, limit)).getContent();
        Map<Long, String> nicknameMap = nicknameResolver.buildMap(suggestions, FestivalSuggestion::getUserId);
        return suggestions.stream()
                .map(s -> FestivalSuggestionResponseDto.from(s, nicknameMap.getOrDefault(s.getUserId(), UserNicknameLookup.UNKNOWN)))
                .toList();
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void approve(Long suggestionId, Long festivalId) {
        FestivalSuggestion suggestion = EntityLoader.getOrThrow(suggestionRepository::findById, suggestionId, "페스티벌 신청");
        requirePending(suggestion);
        suggestion.approve(festivalId);
        eventPublisher.publishEvent(new FestivalSuggestionProcessedEvent(
                suggestion.getUserId(), festivalId, suggestion.getFestivalName(), null));
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void dismiss(Long suggestionId, String processNote) {
        FestivalSuggestion suggestion = EntityLoader.getOrThrow(suggestionRepository::findById, suggestionId, "페스티벌 신청");
        requirePending(suggestion);
        suggestion.dismiss(processNote);
        eventPublisher.publishEvent(new FestivalSuggestionProcessedEvent(
                suggestion.getUserId(), null, suggestion.getFestivalName(), processNote));
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        suggestionRepository.deleteByUserId(userId);
    }

    // 이중 클릭·요청 재시도로 동일 신청이 두 번 승인/반려되며 알림이 중복 발송되는 것을 방지
    private void requirePending(FestivalSuggestion suggestion) {
        if (!suggestion.isPending()) {
            throw new IllegalArgumentException("이미 처리된 페스티벌 신청입니다.");
        }
    }
}
