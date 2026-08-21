package com.feple.feple_backend.ticketlink.entity;

import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "festival_ticket_link", indexes = {
    @Index(name = "idx_ticket_link_festival_id", columnList = "festival_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FestivalTicketLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(length = 100)
    private String label;

    @Column(nullable = false, length = 500)
    private String url;

    public Long getFestivalId() {
        return festival != null ? festival.getId() : null;
    }
}
