package com.feple.feple_backend.stage.entity;

import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
