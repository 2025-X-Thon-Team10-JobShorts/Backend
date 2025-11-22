package com.xhackathon.server.domain.shortform.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiCallbackRequest {
    private String jobId;
    private String status; // SUCCESS, FAILED
    private AiResult result;
    private String error;
    
    @Getter
    @Setter
    public static class AiResult {
        private String transcript;
        private String summary;
        private String[] keywords;
        private Double processingTime;
    }
}