package com.xhackathon.server.domain.shortform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AwsS3Service {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final ThumbnailGeneratorService thumbnailGeneratorService;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public AwsS3Service(ThumbnailGeneratorService thumbnailGeneratorService, ObjectMapper objectMapper) {
        this.presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();

        this.s3Client = S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();

        this.thumbnailGeneratorService = thumbnailGeneratorService;
        this.objectMapper = objectMapper;
    }

    public String generateVideoKey(String ownerPid, String fileName) {
        String uuid = UUID.randomUUID().toString();
        return "videos/" + ownerPid + "/" + uuid + "_" + fileName;
    }

    public URL generatePresignedUploadUrl(String key, String mimeType) {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(mimeType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        return presigner.presignPutObject(presignRequest).url();
    }

    public String getSummary(Long shortFormId, String videoKey) {
        String summaryJson = getSummaryRaw(videoKey);
        if (summaryJson == null) {
            String originalName = videoKey.split("_", 2)[1];
            String baseName = originalName.replace(".mp4", "");
            String key = "summary/summary_" + baseName + ".json";
            throw new RuntimeException("[S3] Summary 파일 읽기 실패: " + key);
        }
        return summaryJson;
    }


    public String generateVideoUrl(String videoKey) {
        // CloudFront 또는 S3 URL 생성
        return "https://cdn.example.com/" + videoKey;
    }

    public String generateThumbnailKey(String videoKey) {
        // 비디오 키에서 썸네일 키 생성 (확장자만 변경)
        String baseName = videoKey.replace(".mp4", "");
        return baseName + "_thumbnail.jpg";
    }

    public boolean generateThumbnail(String videoKey) {
        String thumbnailKey = generateThumbnailKey(videoKey);
        
        try {
            log.info("썸네일 생성 시작: {} -> {}", videoKey, thumbnailKey);
            
            // 1. S3에서 비디오 파일 존재 확인
            if (!isVideoFileExists(videoKey)) {
                log.error("비디오 파일이 존재하지 않음: {}", videoKey);
                return false;
            }
            
            // 2. FFmpeg를 사용한 실제 썸네일 생성
            boolean success = thumbnailGeneratorService.generateThumbnailFromS3Video(
                    bucket, videoKey, thumbnailKey
            );
            
            if (success) {
                log.info("썸네일 생성 성공: {} -> {}", videoKey, thumbnailKey);
                return true;
            } else {
                log.error("썸네일 생성 실패: {}", videoKey);
                return false;
            }
            
        } catch (Exception e) {
            log.error("썸네일 생성 중 오류: {} - {}", videoKey, e.getMessage(), e);
            return false;
        }
    }
    
    private boolean isVideoFileExists(String videoKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(videoKey)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            log.debug("비디오 파일 존재 확인: {}", videoKey);
            return true;
            
        } catch (NoSuchKeyException e) {
            log.warn("비디오 파일이 존재하지 않음: {}", videoKey);
            return false;
        } catch (S3Exception e) {
            log.error("S3 파일 존재 확인 실패: {} - {}", videoKey, e.getMessage());
            return false;
        }
    }

    public String getThumbnailUrl(String thumbnailKey) {
        if (thumbnailKey == null) {
            return null;
        }
        return "https://cdn.example.com/" + thumbnailKey;
    }

    /**
     * S3 Summary 파일에서 태그(Keywords) 정보 추출
     * 
     * @param videoKey 비디오 키
     * @return 태그 목록 (없으면 빈 리스트 반환)
     */
    public List<String> extractTagsFromSummary(String videoKey) {
        try {
            // Summary 파일 읽기
            String summaryJson = getSummaryRaw(videoKey);
            if (summaryJson == null || summaryJson.trim().isEmpty()) {
                log.warn("Summary 파일이 비어있음: {}", videoKey);
                return Collections.emptyList();
            }

            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(summaryJson);
            List<String> tags = new ArrayList<>();

            // 1. keywords 필드 확인 (AI 콜백에서 사용하는 필드)
            if (jsonNode.has("keywords") && jsonNode.get("keywords").isArray()) {
                for (JsonNode keywordNode : jsonNode.get("keywords")) {
                    if (keywordNode.isTextual()) {
                        tags.add(keywordNode.asText());
                    }
                }
            }

            // 2. tags 필드 확인 (대체 필드)
            if (tags.isEmpty() && jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
                for (JsonNode tagNode : jsonNode.get("tags")) {
                    if (tagNode.isTextual()) {
                        tags.add(tagNode.asText());
                    }
                }
            }

            // 3. extraJson 내부의 keywords 확인 (extraJson이 문자열인 경우)
            if (tags.isEmpty() && jsonNode.has("extraJson")) {
                String extraJsonStr = jsonNode.get("extraJson").asText();
                if (extraJsonStr != null && !extraJsonStr.isEmpty() && extraJsonStr.startsWith("{")) {
                    try {
                        JsonNode extraJsonNode = objectMapper.readTree(extraJsonStr);
                        if (extraJsonNode.has("keywords") && extraJsonNode.get("keywords").isArray()) {
                            for (JsonNode keywordNode : extraJsonNode.get("keywords")) {
                                if (keywordNode.isTextual()) {
                                    tags.add(keywordNode.asText());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("extraJson 파싱 실패: {}", e.getMessage());
                    }
                }
            }

            log.info("Summary에서 태그 추출 완료 - videoKey: {}, tags: {}", videoKey, tags);
            return tags;

        } catch (Exception e) {
            log.error("Summary에서 태그 추출 실패 - videoKey: {}, error: {}", videoKey, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Summary 파일을 읽어서 원본 JSON 문자열 반환 (내부 메서드)
     */
    private String getSummaryRaw(String videoKey) {
        try {
            String originalName = videoKey.split("_", 2)[1];
            String baseName = originalName.replace(".mp4", "");
            String key = "summary/summary_" + baseName + ".json";

            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(req)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("Summary 파일 읽기 실패 (파일이 없을 수 있음): {}", videoKey);
            return null;
        }
    }
}