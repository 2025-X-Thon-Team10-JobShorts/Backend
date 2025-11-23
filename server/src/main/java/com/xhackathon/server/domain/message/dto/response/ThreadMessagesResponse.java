package com.xhackathon.server.domain.message.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ThreadMessagesResponse {
    private ThreadResponse thread;
    private List<MessageResponse> messages;

    public ThreadMessagesResponse(ThreadResponse thread, List<MessageResponse> messages) {
        this.thread = thread;
        this.messages = messages;
    }
}