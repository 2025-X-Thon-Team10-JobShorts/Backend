package com.xhackathon.server.domain.message.repository;

import com.xhackathon.server.domain.message.entity.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {
    
    @Query("SELECT t FROM Thread t WHERE (t.participant1Pid = :userPid OR t.participant2Pid = :userPid) ORDER BY t.updatedAt DESC")
    List<Thread> findThreadsByUserPid(@Param("userPid") String userPid);
    
    @Query("SELECT t FROM Thread t WHERE " +
           "(t.participant1Pid = :user1Pid AND t.participant2Pid = :user2Pid) OR " +
           "(t.participant1Pid = :user2Pid AND t.participant2Pid = :user1Pid)")
    Optional<Thread> findThreadBetweenUsers(@Param("user1Pid") String user1Pid, @Param("user2Pid") String user2Pid);
    
    @Query("SELECT t FROM Thread t WHERE t.participant1Pid = :userPid OR t.participant2Pid = :userPid")
    List<Thread> findAllThreadsByUserPid(@Param("userPid") String userPid);
}