package com.xhackathon.server.domain.shortform.controller;

import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.entity.UserRole;
import com.xhackathon.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/short-forms/test")
public class TestDataController {

    private final ShortFormRepository shortFormRepository;
    private final UserRepository userRepository;

    @PostMapping("/create-test-data")
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

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getDataCount() {
        long shortFormCount = shortFormRepository.count();
        long userCount = userRepository.count();
        
        return ResponseEntity.ok(Map.of(
            "shortFormCount", shortFormCount,
            "userCount", userCount
        ));
    }
}