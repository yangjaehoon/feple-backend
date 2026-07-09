package com.feple.feple_backend.admin.checklist;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "festival_checklist")
public class FestivalChecklist {

    static final List<String> ALL_FIELDS = List.of("lineup1", "lineup2", "lineup3", "boothMap", "timetable");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "festival_id", nullable = false, unique = true)
    private Long festivalId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "festival_checklist_item", joinColumns = @JoinColumn(name = "checklist_id"))
    @MapKeyColumn(name = "item_key", length = 20)
    @Column(name = "checked")
    private Map<String, Boolean> items = new HashMap<>();

    @Column(columnDefinition = "TEXT")
    private String memo;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static FestivalChecklist of(Long festivalId) {
        FestivalChecklist checklist = new FestivalChecklist();
        checklist.festivalId = festivalId;
        return checklist;
    }

    public boolean toggle(String field) {
        if (!ALL_FIELDS.contains(field)) {
            throw new IllegalArgumentException("알 수 없는 항목: " + field);
        }
        boolean next = !items.getOrDefault(field, false);
        items.put(field, next);
        return next;
    }

    public int getFieldCount() {
        return ALL_FIELDS.size();
    }

    public int getCompletedCount() {
        return (int) ALL_FIELDS.stream().filter(f -> Boolean.TRUE.equals(items.get(f))).count();
    }

    public boolean isAllCompleted() {
        return ALL_FIELDS.stream().allMatch(f -> Boolean.TRUE.equals(items.get(f)));
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public boolean valueOf(String field) {
        return Boolean.TRUE.equals(items.get(field));
    }

    // Thymeleaf ${cl.lineup1} 등 기존 템플릿 접근 지원
    public boolean isLineup1()   { return valueOf("lineup1"); }
    public boolean isLineup2()   { return valueOf("lineup2"); }
    public boolean isLineup3()   { return valueOf("lineup3"); }
    public boolean isBoothMap()  { return valueOf("boothMap"); }
    public boolean isTimetable() { return valueOf("timetable"); }
}
