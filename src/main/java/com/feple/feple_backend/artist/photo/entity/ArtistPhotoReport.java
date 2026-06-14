package com.feple.feple_backend.artist.photo.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.Resolvable;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "artist_photo_report",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reporter_id", "photo_id"})
    }
)
public class ArtistPhotoReport extends BaseTimeEntity implements Resolvable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private ArtistGalleryPhoto photo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column
    private String detail;

    public void resolve(ReportStatus newStatus) {
        this.status = newStatus;
    }

    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }

    public Long getPhotoId() { return photo.getId(); }
    public String getPhotoTitle() { return photo.getTitle(); }
    public String getPhotoArtistName() { return photo.getArtist().getName(); }
    public Long getReporterId() { return reporter.getId(); }
    public String getReporterNickname() { return reporter.getNickname(); }
    public Long getPhotoUploaderId() { return photo.getUploaderId(); }
    public String getPhotoUploaderNickname() { return photo.getUploaderNickname(); }
}
