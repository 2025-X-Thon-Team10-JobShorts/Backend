package com.xhackathon.server.domain.mypage.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "applicant_bookmarks")
public class ApplicantBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_pid", nullable = false)
    private String companyPid;

    @Column(name = "applicant_pid", nullable = false)
    private String applicantPid;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ApplicantBookmark() {}

    public ApplicantBookmark(String companyPid, String applicantPid) {
        this.companyPid = companyPid;
        this.applicantPid = applicantPid;
        this.createdAt = OffsetDateTime.now();
    }
}