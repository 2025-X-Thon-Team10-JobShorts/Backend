package com.xhackathon.server.domain.shortform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AiCallbackRequest {
    private String jobId;
    
    // status: "DONE" (AI 코드) 또는 "SUCCESS" / "FAILED" (백엔드)
    private String status;
    
    // S3 정보 (AI 코드에서 전송)
    @JsonProperty("s3_bucket")
    private String s3Bucket;
    
    @JsonProperty("s3_key")
    private String s3Key;
    
    // Flat 구조: transcript와 summary가 최상위에 있음 (AI 코드)
    private String transcript;
    private String summary;
    
    // result 객체 (nested 구조, 하위 호환성)
    private AiResult result;
    
    // result_s3_key: S3에 업로드한 summary JSON 파일 키
    @JsonProperty("result_s3_key")
    private String resultS3Key;
    
    // meta 정보 (duration_ms, model, stt_engine)
    private Map<String, Object> meta;
    
    // 에러 정보 (실패 시)
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    // 기존 error 필드 (하위 호환성)
    private String error;
    
    @Getter
    @Setter
    public static class AiResult {
        private String transcript;
        private String summary;
        private String[] keywords;
        
        @JsonProperty("processing_time")
        private Double processingTime;
    }
}