package com.feple.feple_backend.admin.checklist;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "festival_checklist")
public class FestivalChecklist {


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

    public void toggle(String field) {
        String key = ChecklistField.fromKey(field).getKey();
        items.put(key, !items.getOrDefault(key, false));
    }

    public int getFieldCount() {
        return ChecklistField.values().length;
    }

    public int getCompletedCount() {
        return (int) Arrays.stream(ChecklistField.values())
                .filter(f -> Boolean.TRUE.equals(items.get(f.getKey()))).count();
    }

    public boolean isAllCompleted() {
        return Arrays.stream(ChecklistField.values())
                .allMatch(f -> Boolean.TRUE.equals(items.get(f.getKey())));
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public boolean isChecked(String field) {
        return Boolean.TRUE.equals(items.get(field));
    }

    // Thymeleaf ${cl.lineup1} 등 기존 템플릿 접근 지원
    public boolean isLineup1()   { return isChecked("lineup1"); }
    public boolean isLineup2()   { return isChecked("lineup2"); }
    public boolean isLineup3()   { return isChecked("lineup3"); }
    public boolean isBoothMap()  { return isChecked("boothMap"); }
    public boolean isTimetable() { return isChecked("timetable"); }
}
