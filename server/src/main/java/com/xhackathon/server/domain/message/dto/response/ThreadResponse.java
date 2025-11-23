package com.xhackathon.server.domain.message.dto.response;

import com.xhackathon.server.domain.message.entity.Thread;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ThreadResponse {
    private Long id;
    private String participant1Pid;
    private String participant2Pid;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long unreadCount;

    public static ThreadResponse from(Thread thread) {
        ThreadResponse response = new ThreadResponse();
        response.id = thread.getId();
        response.participant1Pid = thread.getParticipant1Pid();
        response.participant2Pid = thread.getParticipant2Pid();
        response.createdAt = thread.getCreatedAt();
        response.updatedAt = thread.getUpdatedAt();
        return response;
    }

    public static ThreadResponse from(Thread thread, Long unreadCount) {
        ThreadResponse response = from(thread);
        response.unreadCount = unreadCount;
        return response;
    }
}