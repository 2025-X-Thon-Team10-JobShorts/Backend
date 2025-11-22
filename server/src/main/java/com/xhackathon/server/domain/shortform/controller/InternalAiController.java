package com.xhackathon.server.domain.shortform.controller;

import com.xhackathon.server.domain.shortform.dto.request.AiCallbackRequest;
import com.xhackathon.server.domain.shortform.dto.response.AiCallbackResponse;
import com.xhackathon.server.domain.shortform.service.AiCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/jobs")
public class InternalAiController {

    private final AiCallbackService aiCallbackService;

    /**
     * AI Pod에서 처리 완료 콜백을 수신하는 엔드포인트
     * 
     * @param encodedJobId URL 인코딩된 Job ID (S3 키)
     * @param request 콜백 요청 데이터
     * @return 처리 결과
     */
    @PostMapping("/{jobId:.*}/complete")
    public ResponseEntity<AiCallbackResponse> handleAiCallback(
            @PathVariable("jobId") String encodedJobId,
            @RequestBody AiCallbackRequest request
    ) {
        try {
            // URL 디코딩하여 실제 S3 키 복원
            String decodedJobId = URLDecoder.decode(encodedJobId, StandardCharsets.UTF_8);
            
            log.info("AI 콜백 수신 - jobId: {}, status: {}", decodedJobId, request.getStatus());
            
            // jobId 일치 확인
            if (!decodedJobId.equals(request.getJobId())) {
                log.warn("jobId 불일치 - URL: {}, Body: {}", decodedJobId, request.getJobId());
                return ResponseEntity.badRequest()
                        .body(AiCallbackResponse.failure("jobId 불일치"));
            }
            
            // AI 콜백 처리
            boolean success = aiCallbackService.processAiCallback(decodedJobId, request);
            
            if (success) {
                return ResponseEntity.ok(AiCallbackResponse.success("AI 콜백 처리 완료"));
            } else {
                return ResponseEntity.internalServerError()
                        .body(AiCallbackResponse.failure("AI 콜백 처리 실패"));
            }
            
        } catch (Exception e) {
            log.error("AI 콜백 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(AiCallbackResponse.failure("서버 오류: " + e.getMessage()));
        }
    }
    
    /**
     * AI 처리 상태 확인용 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AI Callback Service is running");
    }
}