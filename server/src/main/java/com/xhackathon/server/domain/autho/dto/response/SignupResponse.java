package com.xhackathon.server.domain.autho.dto.response;

import lombok.Getter;

@Getter
public class SignupResponse {

    private String pid;

    public SignupResponse(String pid) {
        this.pid = pid;
    }

}