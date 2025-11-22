package com.xhackathon.server.domain.shortform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final ThumbnailGeneratorService thumbnailGeneratorService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

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

        String originalName = videoKey.split("_", 2)[1];
        String baseName = originalName.replace(".mp4", "");

        String key = "summary/summary_" + baseName + ".json";

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(req)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("[S3] Summary 파일 읽기 실패: " + key, e);
        }
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
}