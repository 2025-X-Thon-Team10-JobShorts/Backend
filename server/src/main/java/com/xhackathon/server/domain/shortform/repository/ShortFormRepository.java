package com.xhackathon.server.domain.shortform.repository;

import com.xhackathon.server.domain.shortform.entity.ShortForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ShortFormRepository extends JpaRepository<ShortForm, Long> {

    List<ShortForm> findByOwnerPid(String ownerPid);
    
    List<ShortForm> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<ShortForm> findByIdLessThanOrderByCreatedAtDesc(Long id, Pageable pageable);
}
