package com.feple.feple_backend.admin.ocr;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnmatchedArtistSuggestionService {

    private final UnmatchedArtistSuggestionRepository repository;

    @Transactional
    public void saveAll(List<String> names) {
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            if (repository.incrementMentionCountByNameIgnoreCase(trimmed) == 0) {
                // 완전히 새로운 이름을 두 OCR 배치가 동시에 처음 언급하면 increment가 둘 다 0건으로
                // 나와 save()가 유니크 제약(name)에서 경합할 수 있다. 상대가 이미 저장했다는 뜻이므로
                // 이 언급은 집계에서 누락하고 나머지 이름 처리를 계속한다(mentionCount는 정렬용 참고
                // 지표라 드문 경합으로 인한 1건 누락이 치명적이지 않음).
                try {
                    repository.save(UnmatchedArtistSuggestion.of(trimmed));
                } catch (DataIntegrityViolationException e) {
                    // 무시하고 다음 이름 계속 처리
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<UnmatchedArtistSuggestionDto> getAll() {
        return repository.findAllOrderByMentionCountDesc()
                .stream().map(UnmatchedArtistSuggestionDto::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
