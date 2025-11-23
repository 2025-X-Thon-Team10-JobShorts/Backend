package com.xhackathon.server.domain.shortform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "short_form_ai")
public class ShortFormAi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_form_id", nullable = false)
    private Long shortFormId;

    @Column(name = "stt_provider", length = 64, nullable = true)
    private String sttProvider;

    @Column(name = "stt_request_id", length = 255)
    private String sttRequestId;

    @Column(columnDefinition = "text")
    private String transcript;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "extra_json", columnDefinition = "jsonb")
    private String extraJson;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ai_job_status")
    private ShortFormAiStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ShortFormAi() {}

    public static ShortFormAi createPending(Long shortFormId) {
        ShortFormAi ai = new ShortFormAi();
        ai.shortFormId = shortFormId;
        ai.status = ShortFormAiStatus.PENDING;
        ai.createdAt = OffsetDateTime.now();
        ai.updatedAt = ai.createdAt;
        return ai;
    }

    public void updateSuccess(String transcript, String summary, String extraJson) {
        this.transcript = transcript;
        this.summary = summary;
        this.extraJson = extraJson;
        this.status = ShortFormAiStatus.DONE;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateError(String message) {
        this.status = ShortFormAiStatus.FAILED;
        this.errorMessage = message;
        this.updatedAt = OffsetDateTime.now();
    }
}
