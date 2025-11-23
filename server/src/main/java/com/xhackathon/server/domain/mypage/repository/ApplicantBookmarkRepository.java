package com.xhackathon.server.domain.mypage.repository;

import com.xhackathon.server.domain.mypage.entity.ApplicantBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApplicantBookmarkRepository extends JpaRepository<ApplicantBookmark, Long> {

    @Query("SELECT ab FROM ApplicantBookmark ab WHERE ab.companyPid = :companyPid ORDER BY ab.createdAt DESC")
    Page<ApplicantBookmark> findByCompanyPidOrderByCreatedAtDesc(@Param("companyPid") String companyPid, Pageable pageable);

    Optional<ApplicantBookmark> findByCompanyPidAndApplicantPid(String companyPid, String applicantPid);

    boolean existsByCompanyPidAndApplicantPid(String companyPid, String applicantPid);

    void deleteByCompanyPidAndApplicantPid(String companyPid, String applicantPid);

    long countByCompanyPid(String companyPid);
}