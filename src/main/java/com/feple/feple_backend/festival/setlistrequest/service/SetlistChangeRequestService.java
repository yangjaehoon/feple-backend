package com.feple.feple_backend.festival.setlistrequest.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.setlistrequest.entity.SetlistChangeRequest;
import com.feple.feple_backend.festival.setlistrequest.entity.SetlistChangeRequestStatus;
import com.feple.feple_backend.festival.setlistrequest.repository.SetlistChangeRequestRepository;
import com.feple.feple_backend.global.EntityFinder;
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

    @Transactional
    public void submit(Long userId, Long festivalId, Long artistFestivalId,
                       String artistName, String message) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        repository.save(SetlistChangeRequest.of(user, festivalId, artistFestivalId, artistName, festival.getTitle(), message));
    }

    @Transactional(readOnly = true)
    public Page<SetlistChangeRequest> list(SetlistChangeRequestStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByStatus(SetlistChangeRequestStatus.PENDING);
    }

    @Transactional
    public void resolve(Long requestId) {
        SetlistChangeRequest req = EntityFinder.getOrThrow(repository::findById, requestId, "셋리스트 수정 요청");
        req.resolve();
    }
}
