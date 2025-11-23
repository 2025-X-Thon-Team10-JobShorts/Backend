package com.xhackathon.server.domain.bookmark.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "bookmark")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_pid", nullable = false)
    private String companyPid;

    @Column(name = "short_form_id", nullable = false)
    private Long shortFormId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Bookmark() {}

    public Bookmark(String companyPid, Long shortFormId) {
        this.companyPid = companyPid;
        this.shortFormId = shortFormId;
        this.createdAt = OffsetDateTime.now();
    }
}
