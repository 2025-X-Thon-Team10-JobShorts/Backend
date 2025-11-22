package com.xhackathon.server.domain.shortform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "short_forms")
public class ShortForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_pid", nullable = false)
    private String ownerPid;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "video_key")
    private String videoKey;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "short_form_status")
    private ShortFormStatus status;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "visibility_type")
    private VisibilityType visibility;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ShortForm() {}

    public ShortForm(String ownerPid,
                     String title,
                     String description,
                     String videoKey,
                     Integer durationSec) {
        this.ownerPid = ownerPid;
        this.title = title;
        this.description = description;
        this.videoKey = videoKey;
        this.durationSec = durationSec;
        this.status = ShortFormStatus.READY;   // 기본값 지정
        this.visibility = VisibilityType.PUBLIC;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void updateThumbnail(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateStatus(ShortFormStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }
}

