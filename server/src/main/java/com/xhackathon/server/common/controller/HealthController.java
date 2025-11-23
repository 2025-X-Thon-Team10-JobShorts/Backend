package com.xhackathon.server.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 루트 헬스체크 컨트롤러 (AI 코드가 /health를 호출)
 */
@Slf4j
@RestController
public class HealthController {
    
    @Value("${app.internal.token:}")
    private String internalToken;
    
    @GetMapping("/health")
    public ResponseEntity<String> health(
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

