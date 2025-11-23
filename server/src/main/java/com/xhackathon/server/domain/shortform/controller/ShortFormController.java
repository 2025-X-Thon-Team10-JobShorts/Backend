package com.xhackathon.server.domain.shortform.controller;

import com.xhackathon.server.domain.shortform.dto.request.ShortFormCreateRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormUploadUrlRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormReelsRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormFeedRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormSearchRequest;
import com.xhackathon.server.domain.shortform.dto.response.*;
import com.xhackathon.server.domain.shortform.service.ShortFormService;
import com.xhackathon.server.domain.shortform.service.S3CrawlingService;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.entity.UserRole;
import com.xhackathon.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/short-forms")
public class ShortFormController {

    private final ShortFormService shortFormService;
    private final S3CrawlingService s3CrawlingService;
    private final ShortFormRepository shortFormRepository;
    private final UserRepository userRepository;

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

    @GetMapping("/api/feed")
    public ResponseEntity<ShortFormFeedResponse> getShortFormFeedGet(
            @RequestParam(required = false) String pageParam,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String currentUserPid
    ) {
        // Feed 조회 시 DB에 데이터가 없으면 자동으로 S3 크롤링 실행
        try {
            long shortFormCount = shortFormRepository.count();
            if (shortFormCount == 0) {
                log.info("DB에 ShortForm 데이터가 없음. S3 크롤링 실행");
                s3CrawlingService.crawlS3SummaryAndMapVideoUsers("summary/");
                log.info("S3 크롤링 완료");
            }
        } catch (Exception e) {
            log.warn("Feed 조회 중 S3 크롤링 실패, 계속 진행: {}", e.getMessage());
        }
        
        return ResponseEntity.ok(shortFormService.getFeed(pageParam, size, currentUserPid));
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

    /**
     * 테스트 데이터 생성
     */
    @PostMapping("/admin/create-test-data")
    public ResponseEntity<Map<String, Object>> createTestData() {
        try {
            // 테스트 사용자 생성
            createTestUsers();
            
            // 테스트 ShortForm 데이터 생성  
            List<ShortForm> testShortForms = Arrays.asList(
                new ShortForm("testuser1", "Test Video 1", "Test Description 1", 
                             "videos/testuser1/uuid1_testvideo1.mp4", 30, 
                             Arrays.asList("test", "demo", "video")),
                new ShortForm("testuser2", "Test Video 2", "Test Description 2",
                             "videos/testuser2/uuid2_testvideo2.mp4", 45,
                             Arrays.asList("sample", "test")),
                new ShortForm("testuser1", "Test Video 3", "Test Description 3",
                             "videos/testuser1/uuid3_testvideo3.mp4", 60,
                             Arrays.asList("demo", "showcase"))
            );

            shortFormRepository.saveAll(testShortForms);
            
            log.info("테스트 데이터 생성 완료: {}개 ShortForm 생성됨", testShortForms.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "테스트 데이터 생성 완료",
                "count", testShortForms.size()
            ));
            
        } catch (Exception e) {
            log.error("테스트 데이터 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "테스트 데이터 생성 실패: " + e.getMessage()
            ));
        }
    }

    private void createTestUsers() {
        try {
            // 이미 존재하는지 확인 후 생성
            if (userRepository.findByPid("testuser1").isEmpty()) {
                User user1 = new User("testuser1", "testuser1", "password123", UserRole.JOB_SEEKER, "Test User 1");
                userRepository.save(user1);
            }
            
            if (userRepository.findByPid("testuser2").isEmpty()) {
                User user2 = new User("testuser2", "testuser2", "password123", UserRole.JOB_SEEKER, "Test User 2");
                userRepository.save(user2);
            }
            
            log.info("테스트 사용자 생성/확인 완료");
        } catch (Exception e) {
            log.warn("테스트 사용자 생성 중 오류 (기존 데이터가 있을 수 있음): {}", e.getMessage());
        }
    }
}
