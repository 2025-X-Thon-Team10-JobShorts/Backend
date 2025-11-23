package com.xhackathon.server.domain.shortform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class S3CrawlingServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private ShortFormRepository shortFormRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AwsS3Service awsS3Service;

    @Mock
    private ThumbnailGeneratorService thumbnailGeneratorService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private S3CrawlingService s3CrawlingService;

    @Test
    void contextLoads() {
        assertNotNull(s3CrawlingService);
    }

    // 실제 테스트는 환경 설정 후 진행
    // @Test
    // void testCrawlS3SummaryAndMapVideoUsers() {
    //     // Given
    //     String summaryPrefix = "summary/";
    //     
    //     // When & Then
    //     // 실제 S3 환경에서 테스트 진행
    // }
}