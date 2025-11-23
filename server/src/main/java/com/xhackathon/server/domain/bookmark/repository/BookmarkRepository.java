package com.xhackathon.server.domain.bookmark.repository;

import com.xhackathon.server.domain.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByCompanyPidAndShortFormId(String companyPid, Long shortFormId);

    Optional<Bookmark> findByCompanyPidAndShortFormId(String companyPid, Long shortFormId);

    List<Bookmark> findByCompanyPid(String companyPid);
}
