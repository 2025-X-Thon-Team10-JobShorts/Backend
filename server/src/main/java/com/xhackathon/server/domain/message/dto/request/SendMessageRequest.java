package com.xhackathon.server.domain.message.dto.request;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String senderPid;
    private String content;
}