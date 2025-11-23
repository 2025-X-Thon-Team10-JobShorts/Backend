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
    
    @Query(value = "SELECT * FROM short_form s WHERE " +
           "(:tag IS NULL OR s.tags @> CAST('\"' || :tag || '\"' AS jsonb)) " +
           "ORDER BY s.created_at DESC", 
           nativeQuery = true)
    List<ShortForm> findByTagContaining(@Param("tag") String tag, Pageable pageable);
    
    @Query(value = "SELECT * FROM short_form s WHERE " +
           "(:tag IS NULL OR s.tags @> CAST('\"' || :tag || '\"' AS jsonb)) AND " +
           "s.id < :lastId " +
           "ORDER BY s.created_at DESC",
           nativeQuery = true)
    List<ShortForm> findByTagContainingAndIdLessThan(@Param("tag") String tag, @Param("lastId") Long lastId, Pageable pageable);
    
    List<ShortForm> findByThumbnailKeyIsNull();
}
