package com.xhackathon.server.domain.autho.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {

    private boolean success;
    private String pid;
    private String message;

    public LoginResponse(boolean success, String pid, String message) {
        this.success = success;
        this.pid = pid;
        this.message = message;
    }

}
