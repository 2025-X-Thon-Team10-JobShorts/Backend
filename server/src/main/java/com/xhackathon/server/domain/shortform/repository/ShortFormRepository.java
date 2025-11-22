package com.xhackathon.server.domain.shortform.repository;

import com.xhackathon.server.domain.shortform.entity.ShortForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ShortFormRepository extends JpaRepository<ShortForm, Long> {

    List<ShortForm> findByOwnerPid(String ownerPid);
    
    Optional<ShortForm> findByVideoKey(String videoKey);
    
    List<ShortForm> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<ShortForm> findByIdLessThanOrderByCreatedAtDesc(Long id, Pageable pageable);
    
    @Query("SELECT s FROM ShortForm s WHERE " +
           "(:tag IS NULL OR JSON_CONTAINS(s.tags, JSON_QUOTE(:tag))) " +
           "ORDER BY s.createdAt DESC")
    List<ShortForm> findByTagContaining(@Param("tag") String tag, Pageable pageable);
    
    @Query("SELECT s FROM ShortForm s WHERE " +
           "(:tag IS NULL OR JSON_CONTAINS(s.tags, JSON_QUOTE(:tag))) AND " +
           "s.id < :lastId " +
           "ORDER BY s.createdAt DESC")
    List<ShortForm> findByTagContainingAndIdLessThan(@Param("tag") String tag, @Param("lastId") Long lastId, Pageable pageable);
}
