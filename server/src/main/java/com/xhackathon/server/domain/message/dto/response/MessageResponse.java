package com.xhackathon.server.domain.message.dto.response;

import com.xhackathon.server.domain.message.entity.Message;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageResponse {
    private Long id;
    private String senderPid;
    private String content;
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;

    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        response.id = message.getId();
        response.senderPid = message.getSenderPid();
        response.content = message.getContent();
        response.createdAt = message.getCreatedAt();
        response.readAt = message.getReadAt();
        return response;
    }
}