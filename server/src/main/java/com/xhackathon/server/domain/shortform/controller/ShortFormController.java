package com.xhackathon.server.domain.shortform.controller;

import com.xhackathon.server.domain.shortform.dto.request.ShortFormCreateRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormUploadUrlRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormReelsRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormFeedRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormSearchRequest;
import com.xhackathon.server.domain.shortform.dto.response.*;
import com.xhackathon.server.domain.shortform.service.ShortFormService;
import com.xhackathon.server.domain.shortform.service.S3CrawlingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/short-forms")
public class ShortFormController {

    private final ShortFormService shortFormService;
    private final S3CrawlingService s3CrawlingService;

    @PostMapping("/upload-url")
    public ResponseEntity<ShortFormUploadUrlResponse> getUploadUrl(
            @RequestBody ShortFormUploadUrlRequest request
    ) {
        return ResponseEntity.ok(shortFormService.createUploadUrl(request));
    }

    @PostMapping
    public ResponseEntity<ShortFormResponse> createShortForm(
            @RequestBody ShortFormCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shortFormService.createShortForm(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShortFormDetailResponse> getShortFormDetail(@PathVariable Long id) {
        return ResponseEntity.ok(shortFormService.getDetail(id));
    }

    @PostMapping("/api/{id}")
    public ResponseEntity<ShortFormReelsResponse> getShortFormReels(
            @PathVariable Long id,
            @RequestBody ShortFormReelsRequest request
    ) {
        return ResponseEntity.ok(shortFormService.getReelsDetail(id, request.getCurrentUserPid()));
    }

    @PostMapping("/api/feed")
    public ResponseEntity<ShortFormFeedResponse> getShortFormFeed(
            @RequestBody ShortFormFeedRequest request
    ) {
        return ResponseEntity.ok(shortFormService.getFeed(request.getPageParam(), request.getSize(), request.getCurrentUserPid()));
    }

    @PostMapping("/api/search")
    public ResponseEntity<ShortFormFeedResponse> searchShortFormsByTag(
            @RequestBody ShortFormSearchRequest request
    ) {
        return ResponseEntity.ok(shortFormService.searchByTag(request.getTag(), request.getPageParam(), request.getSize(), request.getCurrentUserPid()));
    }

    /**
     * S3에서 summary 파일들을 크롤링하여 비디오-사용자 매핑
     * 관리자 기능
     */
    @PostMapping("/admin/crawl-s3")
    public ResponseEntity<Map<String, Object>> crawlS3Summary(
            @RequestParam(value = "summaryPrefix", defaultValue = "summary/") String summaryPrefix) {
        
        try {
            int processedCount = s3CrawlingService.crawlS3SummaryAndMapVideoUsers(summaryPrefix);
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "S3 크롤링이 성공적으로 완료되었습니다.",
                    "processedCount", processedCount,
                    "summaryPrefix", summaryPrefix
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "S3 크롤링 중 오류가 발생했습니다: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 썸네일이 없는 모든 비디오에 대해 썸네일 생성 (비동기)
     * 관리자 기능
     */
    @PostMapping("/admin/generate-missing-thumbnails")
    public ResponseEntity<Map<String, Object>> generateMissingThumbnails() {
        
        try {
            CompletableFuture<Integer> future = s3CrawlingService.generateMissingThumbnails();
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "썸네일 생성 작업이 비동기로 시작되었습니다.",
                    "status", "processing"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "썸네일 생성 작업 시작 중 오류가 발생했습니다: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 비디오의 썸네일 강제 재생성
     * 관리자 기능
     */
    @PostMapping("/admin/regenerate-thumbnail")
    public ResponseEntity<Map<String, Object>> regenerateThumbnail(
            @RequestParam String videoKey) {
        
        try {
            s3CrawlingService.generateThumbnailIfNotExists(videoKey);
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "썸네일 재생성 작업이 비동기로 시작되었습니다.",
                    "videoKey", videoKey
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "썸네일 재생성 중 오류가 발생했습니다: " + e.getMessage(),
                    "videoKey", videoKey,
                    "error", e.getClass().getSimpleName()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
