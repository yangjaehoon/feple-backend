package com.feple.feple_backend.admin.checklist;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

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

    // 수동 항목(ChecklistField) + 자동 계산 항목("타임테이블" 1개)을 합친 전체 개수.
    public int getFieldCount() {
        return ChecklistField.values().length + 1;
    }

    // 타임테이블은 실데이터로 자동 계산되어 items 맵에 저장되지 않으므로 호출부(관리자 목록 화면)가
    // 계산한 값을 파라미터로 받는다.
    public int getCompletedCount(boolean timetableAutoComplete) {
        int manualCompleted = (int) Arrays.stream(ChecklistField.values())
                .filter(f -> Boolean.TRUE.equals(items.get(f.getKey()))).count();
        return manualCompleted + (timetableAutoComplete ? 1 : 0);
    }

    public boolean isAllCompleted(boolean timetableAutoComplete) {
        boolean allManualCompleted = Arrays.stream(ChecklistField.values())
                .allMatch(f -> Boolean.TRUE.equals(items.get(f.getKey())));
        return allManualCompleted && timetableAutoComplete;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public boolean isChecked(String field) {
        return Boolean.TRUE.equals(items.get(field));
    }
}
