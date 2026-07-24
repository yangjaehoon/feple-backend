package com.feple.feple_backend.festival.setlistchangerequest.service;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequest;
import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequestStatus;
import com.feple.feple_backend.festival.setlistchangerequest.repository.SetlistChangeRequestRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetlistChangeRequestService {

    private final SetlistChangeRequestRepository repository;
    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final ArtistFestivalRepository artistFestivalRepository;

    @Transactional
    public void submit(Long userId, Long festivalId, Long artistFestivalId, String message) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        ArtistFestival artistFestival = EntityLoader.getOrThrow(
                artistFestivalRepository::findById, artistFestivalId, "아티스트 참여 정보");
        if (!festivalId.equals(artistFestival.getFestivalId())) {
            throw new IllegalArgumentException("해당 페스티벌의 참여 정보가 아닙니다.");
        }
        repository.save(SetlistChangeRequest.of(
                user, festivalId, artistFestivalId, artistFestival.getArtistName(), festival.getTitle(), message));
    }

    @Transactional(readOnly = true)
    public Page<SetlistChangeRequest> list(SetlistChangeRequestStatus status, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return repository.findByStatusAndKeyword(status, keyword, pageable);
        }
        return repository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return repository.countByStatus(SetlistChangeRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countByStatus(SetlistChangeRequestStatus status) {
        return repository.countByStatus(status);
    }

    @EvictAdminPendingCaches
    @Transactional
    public void resolve(Long requestId) {
        SetlistChangeRequest req = EntityLoader.getOrThrow(repository::findById, requestId, "셋리스트 변경 요청");
        req.resolve();
    }
}
