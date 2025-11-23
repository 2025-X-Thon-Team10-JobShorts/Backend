package com.xhackathon.server.domain.shortform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiCallbackResponse {
    private boolean success;
    private String message;
    
    public static AiCallbackResponse success(String message) {
        return new AiCallbackResponse(true, message);
    }
    
    public static AiCallbackResponse failure(String message) {
        return new AiCallbackResponse(false, message);
    }
}