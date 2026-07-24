package com.feple.feple_backend.stage.entity;

import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stage", indexes = {
    @Index(name = "idx_stage_festival_id", columnList = "festival_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int displayOrder;

    public void swapDisplayOrder(Stage other) {
        int savedOrder = this.displayOrder;
        this.displayOrder = other.displayOrder;
        other.displayOrder = savedOrder;
    }

    public Long getFestivalId() {
        return festival.getId();
    }
}
