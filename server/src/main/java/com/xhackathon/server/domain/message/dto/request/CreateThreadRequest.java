package com.xhackathon.server.domain.message.dto.request;

import lombok.Data;

@Data
public class CreateThreadRequest {
    private String participant1Pid;
    private String participant2Pid;
}