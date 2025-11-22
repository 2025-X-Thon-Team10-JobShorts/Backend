package com.xhackathon.server.domain.shortform.controller;

import com.xhackathon.server.domain.shortform.dto.request.AiCallbackRequest;
import com.xhackathon.server.domain.shortform.dto.response.AiCallbackResponse;
import com.xhackathon.server.domain.shortform.service.AiCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    
    @Value("${app.internal.token:}")
    private String internalToken;

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
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody AiCallbackRequest request
    ) {
        try {
            // 내부 토큰 인증
            if (internalToken != null && !internalToken.isEmpty()) {
                if (token == null || !internalToken.equals(token)) {
                    log.warn("내부 토큰 인증 실패 - jobId: {}", encodedJobId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(AiCallbackResponse.failure("인증 실패"));
                }
            }
            
            // URL 디코딩하여 실제 S3 키 복원
            String decodedJobId = URLDecoder.decode(encodedJobId, StandardCharsets.UTF_8);
            
            log.info("AI 콜백 수신 - jobId: {}, status: {}", decodedJobId, request.getStatus());
            
            // status 변환: "DONE" -> "SUCCESS"
            String normalizedStatus = request.getStatus();
            if ("DONE".equalsIgnoreCase(normalizedStatus)) {
                normalizedStatus = "SUCCESS";
                request.setStatus(normalizedStatus);
            }
            
            // jobId 설정: request body에 없으면 URL에서 가져온 값 사용
            if (request.getJobId() == null || request.getJobId().isEmpty()) {
                request.setJobId(decodedJobId);
                log.debug("jobId를 URL에서 설정: {}", decodedJobId);
            } else {
                // jobId 일치 확인 (request body에 있는 경우만)
                if (!decodedJobId.equals(request.getJobId())) {
                    log.warn("jobId 불일치 - URL: {}, Body: {}", decodedJobId, request.getJobId());
                    return ResponseEntity.badRequest()
                            .body(AiCallbackResponse.failure("jobId 불일치"));
                }
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
    public ResponseEntity<String> healthCheck(
            @RequestHeader(value = "X-Internal-Token", required = false) String token
    ) {
        // 내부 토큰 검증 (설정된 경우)
        if (internalToken != null && !internalToken.isEmpty()) {
            if (token == null || !internalToken.equals(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Unauthorized");
            }
        }
        return ResponseEntity.ok("OK");
    }
}