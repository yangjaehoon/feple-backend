package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.entity.TimetableEntryMember;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// TimetableService와 TimetableEntryBatchPersister가 동일한 멤버 동기화 로직을
// 공유하기 위한 순수 정적 유틸리티 — 상태를 갖지 않아 트랜잭션 경계와 무관하게
// 어느 쪽 영속성 컨텍스트에서 호출해도 안전하다.
final class TimetableMemberSync {

    private TimetableMemberSync() {}

    static void sync(ArtistRepository artistRepository, TimetableEntry entry, List<Long> memberArtistIds) {
        if (memberArtistIds == null || memberArtistIds.isEmpty()) {
            entry.replaceMembers(List.of());
            return;
        }
        Map<Long, Artist> artistsById = artistRepository.findAllById(memberArtistIds).stream()
                .collect(Collectors.toMap(Artist::getId, artist -> artist));
        List<TimetableEntryMember> members = memberArtistIds.stream()
                .map(artistsById::get)
                .filter(Objects::nonNull)
                .map(artist -> TimetableEntryMember.builder()
                        .entry(entry)
                        .artist(artist)
                        .artistName(artist.getName())
                        .build())
                .toList();
        entry.replaceMembers(members);
    }
}
