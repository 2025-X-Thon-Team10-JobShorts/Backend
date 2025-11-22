package com.xhackathon.server.domain.user.repository;

import com.xhackathon.server.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByLoginId(String loginId);
    Optional<User> findByPid(String pid);
}