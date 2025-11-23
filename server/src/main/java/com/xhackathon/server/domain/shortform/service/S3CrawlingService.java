package com.xhackathon.server.domain.shortform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormStatus;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3CrawlingService {

    private final S3Client s3Client;
    private final ShortFormRepository shortFormRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final ThumbnailGeneratorService thumbnailGeneratorService;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에서 summary 파일들을 크롤링하여 비디오와 사용자를 매핑
     * 
     * @param summaryPrefix S3 summary 디렉토리 경로 (예: "summary/")
     * @return 처리된 항목 개수
     */
    @Transactional
    public int crawlS3SummaryAndMapVideoUsers(String summaryPrefix) {
        log.info("S3 크롤링 시작 - prefix: {}", summaryPrefix);
        
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(summaryPrefix)
                    .maxKeys(1000)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            List<S3Object> summaryFiles = listResponse.contents();

            log.info("발견된 summary 파일 개수: {}", summaryFiles.size());

            int processedCount = 0;
            for (S3Object summaryFile : summaryFiles) {
                if (summaryFile.key().endsWith(".json")) {
                    try {
                        processedCount += processSummaryFile(summaryFile.key()) ? 1 : 0;
                    } catch (Exception e) {
                        log.error("Summary 파일 처리 실패: {} - {}", summaryFile.key(), e.getMessage(), e);
                    }
                }
            }

            log.info("S3 크롤링 완료 - 총 {}개 파일 처리됨", processedCount);
            return processedCount;

        } catch (Exception e) {
            log.error("S3 크롤링 실패: {}", e.getMessage(), e);
            throw new RuntimeException("S3 크롤링 실패", e);
        }
    }

    /**
     * 개별 summary 파일을 처리하여 비디오-사용자 매핑
     */
    private boolean processSummaryFile(String summaryKey) {
        log.debug("Summary 파일 처리 시작: {}", summaryKey);

        try {
            // 1. Summary 파일에서 데이터 읽기
            SummaryData summaryData = readSummaryFromS3(summaryKey);
            if (summaryData == null) {
                log.warn("Summary 데이터 읽기 실패: {}", summaryKey);
                return false;
            }

            // 2. 해당하는 비디오 파일 찾기
            String videoKey = findCorrespondingVideoKey(summaryKey);
            if (videoKey == null) {
                log.warn("대응하는 비디오 파일을 찾을 수 없음: {}", summaryKey);
                return false;
            }

            // 3. 비디오 키에서 사용자 PID 추출
            String userPid = extractUserPidFromVideoKey(videoKey);
            if (userPid == null) {
                log.warn("비디오 키에서 사용자 PID 추출 실패: {}", videoKey);
                return false;
            }

            // 4. 사용자 존재 확인
            Optional<User> userOpt = userRepository.findByPid(userPid);
            if (userOpt.isEmpty()) {
                log.warn("사용자를 찾을 수 없음: {}", userPid);
                return false;
            }

            // 5. 기존 ShortForm 레코드가 있는지 확인
            Optional<ShortForm> existingOpt = shortFormRepository.findByVideoKey(videoKey);
            if (existingOpt.isPresent()) {
                // 기존 레코드 업데이트
                updateExistingShortForm(existingOpt.get(), summaryData);
                log.info("기존 ShortForm 업데이트: {}", videoKey);
            } else {
                // 새 레코드 생성
                createNewShortForm(userPid, videoKey, summaryData);
                log.info("새 ShortForm 생성: {}", videoKey);
            }

            // 6. 썸네일이 없으면 비동기로 생성
            generateThumbnailIfNotExists(videoKey);

            return true;

        } catch (Exception e) {
            log.error("Summary 파일 처리 중 오류: {} - {}", summaryKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * S3에서 summary 파일 읽기
     */
    private SummaryData readSummaryFromS3(String summaryKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(summaryKey)
                    .build();

            try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(request)) {
                String summaryJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                return parseSummaryData(summaryJson);
            }

        } catch (Exception e) {
            log.error("Summary 파일 읽기 실패: {} - {}", summaryKey, e.getMessage());
            return null;
        }
    }

    /**
     * Summary JSON 데이터 파싱
     */
    private SummaryData parseSummaryData(String summaryJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(summaryJson);
            
            String transcript = extractTextValue(jsonNode, "transcript");
            String summary = extractTextValue(jsonNode, "summary");
            List<String> tags = extractTagsFromJson(jsonNode);

            return new SummaryData(transcript, summary, tags);

        } catch (Exception e) {
            log.error("Summary JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private String extractTextValue(JsonNode jsonNode, String fieldName) {
        if (jsonNode.has(fieldName)) {
            return jsonNode.get(fieldName).asText();
        }
        return "";
    }

    private List<String> extractTagsFromJson(JsonNode jsonNode) {
        List<String> tags = new ArrayList<>();
        
        // keywords 필드에서 태그 추출
        if (jsonNode.has("keywords") && jsonNode.get("keywords").isArray()) {
            for (JsonNode keywordNode : jsonNode.get("keywords")) {
                if (keywordNode.isTextual()) {
                    tags.add(keywordNode.asText());
                }
            }
        }
        
        // tags 필드에서도 추출 (대체)
        if (tags.isEmpty() && jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
            for (JsonNode tagNode : jsonNode.get("tags")) {
                if (tagNode.isTextual()) {
                    tags.add(tagNode.asText());
                }
            }
        }

        return tags;
    }

    /**
     * Summary 키에서 대응하는 비디오 키 찾기
     * 예: summary/summary_video1.json -> videos/user123/uuid_video1.mp4
     */
    private String findCorrespondingVideoKey(String summaryKey) {
        try {
            // summary/summary_filename.json에서 filename 추출
            String fileName = summaryKey.substring(summaryKey.lastIndexOf("/") + 1);
            if (fileName.startsWith("summary_")) {
                fileName = fileName.substring(8); // "summary_" 제거
            }
            fileName = fileName.replace(".json", "");

            // 비디오 디렉토리에서 해당 파일명을 포함하는 키 검색
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix("videos/")
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object object : listResponse.contents()) {
                String videoKey = object.key();
                if (videoKey.contains(fileName) && isVideoFile(videoKey)) {
                    return videoKey;
                }
            }

            log.warn("대응하는 비디오 파일을 찾을 수 없음: {}", fileName);
            return null;

        } catch (Exception e) {
            log.error("비디오 키 찾기 실패: {} - {}", summaryKey, e.getMessage());
            return null;
        }
    }

    private boolean isVideoFile(String key) {
        String[] videoExtensions = {".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv", ".webm"};
        String lowerKey = key.toLowerCase();
        for (String ext : videoExtensions) {
            if (lowerKey.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 비디오 키에서 사용자 PID 추출
     * 예: videos/user123/uuid_filename.mp4 -> user123
     */
    private String extractUserPidFromVideoKey(String videoKey) {
        try {
            String[] parts = videoKey.split("/");
            if (parts.length >= 3 && parts[0].equals("videos")) {
                return parts[1]; // videos/{userPid}/{filename}
            }
            return null;
        } catch (Exception e) {
            log.error("사용자 PID 추출 실패: {} - {}", videoKey, e.getMessage());
            return null;
        }
    }

    /**
     * 기존 ShortForm 레코드 업데이트
     */
    private void updateExistingShortForm(ShortForm shortForm, SummaryData summaryData) {
        if (summaryData.tags != null && !summaryData.tags.isEmpty()) {
            shortForm.updateTags(summaryData.tags);
        }
        
        if (shortForm.getStatus() == ShortFormStatus.PROCESSING_STT) {
            shortForm.updateStatus(ShortFormStatus.READY);
        }

        shortFormRepository.save(shortForm);
    }

    /**
     * 새 ShortForm 레코드 생성
     */
    private void createNewShortForm(String userPid, String videoKey, SummaryData summaryData) {
        String title = generateTitleFromSummary(summaryData.summary);
        String description = summaryData.summary;
        
        ShortForm shortForm = new ShortForm(
                userPid,
                title,
                description,
                videoKey,
                null, // duration은 나중에 설정
                summaryData.tags
        );

        shortFormRepository.save(shortForm);
        
        // 썸네일 생성 시작
        generateThumbnailIfNotExists(videoKey);
    }

    /**
     * summary에서 제목 생성 (첫 50자 + "...")
     */
    private String generateTitleFromSummary(String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            return "무제";
        }
        
        String title = summary.trim();
        if (title.length() > 50) {
            title = title.substring(0, 50) + "...";
        }
        
        return title;
    }

    /**
     * 썸네일이 없으면 생성
     */
    @Async
    public void generateThumbnailIfNotExists(String videoKey) {
        String thumbnailKey = awsS3Service.generateThumbnailKey(videoKey);
        
        try {
            // 썸네일 파일이 이미 존재하는지 확인
            if (thumbnailExists(thumbnailKey)) {
                log.debug("썸네일이 이미 존재함: {}", thumbnailKey);
                return;
            }

            log.info("썸네일 생성 시작: {} -> {}", videoKey, thumbnailKey);
            
            // 썸네일 생성 (1초 정도의 비디오 클립 다운로드 포함)
            boolean success = awsS3Service.generateThumbnail(videoKey);
            
            if (success) {
                log.info("썸네일 생성 성공: {}", thumbnailKey);
                
                // DB 업데이트
                updateShortFormThumbnail(videoKey, thumbnailKey);
            } else {
                log.error("썸네일 생성 실패: {}", videoKey);
            }

        } catch (Exception e) {
            log.error("썸네일 생성 중 오류: {} - {}", videoKey, e.getMessage(), e);
        }
    }

    private boolean thumbnailExists(String thumbnailKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(thumbnailKey)
                    .build();
            
            s3Client.headObject(headRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("썸네일 존재 확인 실패: {} - {}", thumbnailKey, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void updateShortFormThumbnail(String videoKey, String thumbnailKey) {
        Optional<ShortForm> shortFormOpt = shortFormRepository.findByVideoKey(videoKey);
        if (shortFormOpt.isPresent()) {
            ShortForm shortForm = shortFormOpt.get();
            shortForm.updateThumbnail(thumbnailKey);
            shortFormRepository.save(shortForm);
            log.debug("ShortForm 썸네일 업데이트: {} -> {}", videoKey, thumbnailKey);
        } else {
            log.warn("ShortForm을 찾을 수 없음 (썸네일 업데이트): {}", videoKey);
        }
    }

    /**
     * 모든 썸네일이 없는 비디오에 대해 썸네일 생성
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Integer> generateMissingThumbnails() {
        log.info("누락된 썸네일 생성 작업 시작");
        
        List<ShortForm> shortFormsWithoutThumbnail = shortFormRepository.findByThumbnailKeyIsNull();
        log.info("썸네일이 없는 ShortForm 개수: {}", shortFormsWithoutThumbnail.size());
        
        int generatedCount = 0;
        for (ShortForm shortForm : shortFormsWithoutThumbnail) {
            try {
                generateThumbnailIfNotExists(shortForm.getVideoKey());
                generatedCount++;
            } catch (Exception e) {
                log.error("썸네일 생성 실패: {} - {}", shortForm.getVideoKey(), e.getMessage());
            }
        }
        
        log.info("누락된 썸네일 생성 작업 완료 - {}개 생성", generatedCount);
        return CompletableFuture.completedFuture(generatedCount);
    }

    /**
     * Summary 데이터를 담는 내부 클래스
     */
    private static class SummaryData {
        final String transcript;
        final String summary;
        final List<String> tags;

        public SummaryData(String transcript, String summary, List<String> tags) {
            this.transcript = transcript;
            this.summary = summary;
            this.tags = tags != null ? tags : new ArrayList<>();
        }
    }
}