package com.feple.feple_backend.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    private boolean lineup1;
    private boolean lineup2;
    private boolean lineup3;
    private boolean boothMap;
    private boolean timetable;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static FestivalChecklist of(Long festivalId) {
        FestivalChecklist c = new FestivalChecklist();
        c.festivalId = festivalId;
        return c;
    }

    public void toggle(String field) {
        switch (field) {
            case "lineup1"   -> this.lineup1   = !this.lineup1;
            case "lineup2"   -> this.lineup2   = !this.lineup2;
            case "lineup3"   -> this.lineup3   = !this.lineup3;
            case "boothMap"  -> this.boothMap  = !this.boothMap;
            case "timetable" -> this.timetable = !this.timetable;
            default -> throw new IllegalArgumentException("알 수 없는 항목: " + field);
        }
    }

    public int getCompletedCount() {
        int count = 0;
        if (lineup1)   count++;
        if (lineup2)   count++;
        if (lineup3)   count++;
        if (boothMap)  count++;
        if (timetable) count++;
        return count;
    }

    public boolean isAllCompleted() {
        return lineup1 && lineup2 && lineup3 && boothMap && timetable;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public boolean valueOf(String field) {
        return switch (field) {
            case "lineup1"   -> this.lineup1;
            case "lineup2"   -> this.lineup2;
            case "lineup3"   -> this.lineup3;
            case "boothMap"  -> this.boothMap;
            case "timetable" -> this.timetable;
            default -> false;
        };
    }
}
