package com.feple.feple_backend.festival.setlistchangerequest.service;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequest;
import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequestStatus;
import com.feple.feple_backend.festival.setlistchangerequest.repository.SetlistChangeRequestRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
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
    private final BadWordValidator badWordValidator;

    @Transactional
    public void submit(SetlistChangeRequestCommand command) {
        badWordValidator.validate(command.message());
        if (repository.existsByUserIdAndArtistFestivalIdAndStatus(
                command.userId(), command.artistFestivalId(), SetlistChangeRequestStatus.PENDING)) {
            throw new ConflictException("이미 처리 대기 중인 변경 요청이 있습니다.");
        }
        User user = EntityLoader.getOrThrow(userRepository::findById, command.userId(), "사용자");
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, command.festivalId(), "페스티벌");
        ArtistFestival artistFestival = EntityLoader.getOrThrow(
                artistFestivalRepository::findById, command.artistFestivalId(), "아티스트 참여 정보");
        if (!command.festivalId().equals(artistFestival.getFestivalId())) {
            throw new InvalidRequestException("해당 페스티벌의 참여 정보가 아닙니다.");
        }
        repository.save(SetlistChangeRequest.of(user, command.festivalId(), command.artistFestivalId(),
                artistFestival.getArtistName(), festival.getTitle(), command.message()));
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
