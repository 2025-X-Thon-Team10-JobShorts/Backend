package com.xhackathon.server.domain.shortform.repository;


import com.xhackathon.server.domain.shortform.entity.ShortFormAi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortFormAiRepository extends JpaRepository<ShortFormAi, Long> {

    Optional<ShortFormAi> findByShortFormId(Long shortFormId);
}
