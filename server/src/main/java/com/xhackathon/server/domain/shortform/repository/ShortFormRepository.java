package com.xhackathon.server.domain.shortform.repository;

import com.xhackathon.server.domain.shortform.entity.ShortForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortFormRepository extends JpaRepository<ShortForm, Long> {

    List<ShortForm> findByOwnerPid(String ownerPid);
}
