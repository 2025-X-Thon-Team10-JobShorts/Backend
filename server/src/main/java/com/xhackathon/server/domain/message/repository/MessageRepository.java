package com.xhackathon.server.domain.message.repository;

import com.xhackathon.server.domain.message.entity.Message;
import com.xhackathon.server.domain.message.entity.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByThreadOrderByCreatedAtAsc(Thread thread);
    
    List<Message> findByThreadIdOrderByCreatedAtAsc(Long threadId);
    
    @Query("SELECT m FROM Message m WHERE m.thread.id = :threadId AND m.readAt IS NULL AND m.senderPid != :userPid")
    List<Message> findUnreadMessagesInThread(@Param("threadId") Long threadId, @Param("userPid") String userPid);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.thread.id = :threadId AND m.readAt IS NULL AND m.senderPid != :userPid")
    long countUnreadMessagesInThread(@Param("threadId") Long threadId, @Param("userPid") String userPid);
}