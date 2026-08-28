package com.feple.feple_backend.artist.photo.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.entity.ResolvableReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class ArtistGalleryPhotoReport extends BaseTimeEntity implements ResolvableReport {

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
    public String getPhotoArtistName() { return photo.getArtistName(); }
    public String getPhotoKey() { return photo.getS3Key(); }
    public Long getReporterId() { return reporter.getId(); }
    public String getReporterNickname() { return reporter.getNickname(); }
    public Long getPhotoUploaderId() { return photo.getUploaderId(); }
    public String getPhotoUploaderNickname() { return photo.getUploaderNickname(); }
}
