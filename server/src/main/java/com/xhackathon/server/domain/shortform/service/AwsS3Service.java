package com.xhackathon.server.domain.shortform.service;

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
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class AwsS3Service {
    
    // 썸네일 생성을 위한 락 맵 (videoKey별 락)
    private final ConcurrentHashMap<String, ReentrantLock> thumbnailLocks = new ConcurrentHashMap<>();
    // Summary 파일 접근을 위한 락 맵 (videoKey별 락)
    private final ConcurrentHashMap<String, ReentrantLock> summaryLocks = new ConcurrentHashMap<>();

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final ThumbnailGeneratorService thumbnailGeneratorService;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public AwsS3Service(S3Presigner presigner, S3Client s3Client, ThumbnailGeneratorService thumbnailGeneratorService, ObjectMapper objectMapper) {
        this.presigner = presigner;
        this.s3Client = s3Client;
        this.thumbnailGeneratorService = thumbnailGeneratorService;
        this.objectMapper = objectMapper;
        
        log.info("✅ AwsS3Service 초기화 완료 - S3Client와 S3Presigner Bean 주입됨");
    }

    public String generateVideoKey(String ownerPid, String fileName) {
        String uuid = UUID.randomUUID().toString();
        return "videos/" + ownerPid + "/" + uuid + "_" + fileName;
    }

    public URL generatePresignedUploadUrl(String key, String mimeType) {
        log.info("=== 업로드용 Pre-signed URL 생성 시작 ===");
        log.info("입력 파라미터 - 버킷: {}, 키: {}, MIME 타입: {}", bucket, key, mimeType);
        
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(mimeType)
                .build();
        log.info("PutObjectRequest 생성 완료");

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();
        log.info("PutObjectPresignRequest 생성 완료: 유효시간=10분");

        log.info("presigner.presignPutObject() 호출 시작...");
        URL presignedUrl = presigner.presignPutObject(presignRequest).url();
        log.info("업로드용 Pre-signed URL 생성 성공: {}", presignedUrl.toString().substring(0, Math.min(150, presignedUrl.toString().length())) + "...");
        log.info("=== 업로드용 Pre-signed URL 생성 완료 ===");
        return presignedUrl;
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
    
    /**
     * S3 Summary 파일에서 transcript 정보 추출
     * 
     * @param videoKey 비디오 키
     * @return transcript 문자열 (없으면 빈 문자열 반환)
     */
    public String getTranscriptFromSummary(String videoKey) {
        try {
            String summaryJson = getSummaryRaw(videoKey);
            if (summaryJson == null || summaryJson.trim().isEmpty()) {
                log.info("Summary 파일이 비어있음: {}", videoKey);
                return "";
            }

            JsonNode jsonNode = objectMapper.readTree(summaryJson);
            
            if (jsonNode.has("transcript")) {
                return jsonNode.get("transcript").asText();
            }
            
            return "";

        } catch (Exception e) {
            log.error("Summary에서 transcript 추출 실패 - videoKey: {}, error: {}", videoKey, e.getMessage());
            return "";
        }
    }
    
    /**
     * S3 Summary 파일에서 요약 정보 추출 (summary 필드만)
     * 
     * @param videoKey 비디오 키
     * @return summary 문자열 (없으면 빈 문자열 반환)
     */
    public String getSummaryFromSummary(String videoKey) {
        try {
            String summaryJson = getSummaryRaw(videoKey);
            if (summaryJson == null || summaryJson.trim().isEmpty()) {
                log.info("Summary 파일이 비어있음: {}", videoKey);
                return "";
            }

            JsonNode jsonNode = objectMapper.readTree(summaryJson);
            
            if (jsonNode.has("summary")) {
                return jsonNode.get("summary").asText();
            }
            
            return "";

        } catch (Exception e) {
            log.error("Summary에서 summary 추출 실패 - videoKey: {}, error: {}", videoKey, e.getMessage());
            return "";
        }
    }


    /**
     * S3에서 비디오 다운로드를 위한 Pre-signed URL 생성
     * 
     * Pre-signed URL은 임시 접근 URL로, 프론트엔드에서 직접 영상을 재생할 수 있도록 사용됩니다.
     * 보안이 안전하며, 객체가 private이어도 접근 가능합니다.
     * 
     * 생성되는 URL 형식:
     * https://{bucket}.s3.{region}.amazonaws.com/{key}?X-Amz-Algorithm=...&X-Amz-Signature=...
     * 
     * @param videoKey S3 비디오 키 (예: "videos/user123/uuid_video.mp4")
     * @return Pre-signed URL 문자열 (1시간 유효), 실패 시 null
     */
    public String generateVideoUrl(String videoKey) {
        if (videoKey == null || videoKey.trim().isEmpty()) {
            log.warn("비디오 키가 null이거나 비어있음");
            return null;
        }
        
        try {
            log.info("=== 비디오 Pre-signed URL 생성 시작 ===");
            log.info("입력 파라미터 - 버킷: {}, 키: {}", bucket, videoKey);
            
            // 1. GetObjectRequest 생성 (S3 객체 요청 정보)
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(videoKey)
                    .build();
            log.info("GetObjectRequest 생성 완료: bucket={}, key={}", bucket, videoKey);

            // 2. Pre-signed URL 요청 생성 (유효 시간 설정)
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))  // 1시간 유효
                    .getObjectRequest(getObjectRequest)
                    .build();
            log.info("GetObjectPresignRequest 생성 완료: 유효시간=1시간");

            // 3. Pre-signed URL 생성 및 반환
            log.info("presigner.presignGetObject() 호출 시작...");
            URL presignedUrl = presigner.presignGetObject(presignRequest).url();
            String urlString = presignedUrl.toString();
            
            log.info("비디오 Pre-signed URL 생성 성공!");
            log.info("생성된 URL (처음 150자): {}", urlString.substring(0, Math.min(150, urlString.length())));
            log.info("URL 전체 길이: {} bytes", urlString.length());
            log.info("=== 비디오 Pre-signed URL 생성 완료 ===");
            return urlString;
            
        } catch (S3Exception e) {
            log.error("비디오 Pre-signed URL 생성 실패 (S3 에러): 버킷={}, 키={}, 에러코드={}, 메시지={}", 
                     bucket, videoKey, e.awsErrorDetails().errorCode(), e.getMessage());
            return null; // 프론트엔드에서 에러 처리하도록 null 반환
        } catch (Exception e) {
            log.error("비디오 Pre-signed URL 생성 실패 (예상치 못한 에러): 버킷={}, 키={}, 에러={}", 
                     bucket, videoKey, e.getMessage(), e);
            return null; // 프론트엔드에서 에러 처리하도록 null 반환
        }
    }

    public String generateThumbnailKey(String videoKey) {
        // 비디오 키에서 썸네일 키 생성 (확장자만 변경)
        int lastDotIndex = videoKey.lastIndexOf('.');
        String baseName;
        if (lastDotIndex > 0) {
            baseName = videoKey.substring(0, lastDotIndex);
        } else {
            baseName = videoKey;
        }
        return baseName + "_thumbnail.jpg";
    }

    public boolean generateThumbnail(String videoKey) {
        // videoKey별 락 획득
        ReentrantLock lock = thumbnailLocks.computeIfAbsent(videoKey, k -> new ReentrantLock());
        
        lock.lock();
        try {
            String thumbnailKey = generateThumbnailKey(videoKey);
            
            log.info("썸네일 생성 시작 (락 획득): {} -> {}", videoKey, thumbnailKey);
            
            // 이미 썸네일이 존재하는지 확인
            if (isThumbnailExists(thumbnailKey)) {
                log.info("이미 썸네일이 존재함: {}", thumbnailKey);
                return true;
            }
            
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
        } finally {
            lock.unlock();
            // 안전한 락 정리: 현재 스레드가 보유한 락만 제거
            thumbnailLocks.computeIfPresent(videoKey, (key, existingLock) -> {
                if (existingLock == lock && !lock.hasQueuedThreads()) {
                    return null; // 제거
                }
                return existingLock; // 유지
            });
        }
    }
    
    private boolean isThumbnailExists(String thumbnailKey) {
        log.info("=== 썸네일 파일 존재 확인 시작 ===");
        log.info("입력 파라미터 - 버킷: {}, 키: {}", bucket, thumbnailKey);
        
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(thumbnailKey)
                    .build();
            log.info("HeadObjectRequest 생성 완료");
            
            log.info("s3Client.headObject() 호출 시작...");
            s3Client.headObject(headObjectRequest);
            log.info("썸네일 파일 존재 확인 성공: {}", thumbnailKey);
            log.info("=== 썸네일 파일 존재 확인 완료 ===");
            return true;
            
        } catch (NoSuchKeyException e) {
            log.info("❌ 썸네일 파일이 존재하지 않음: {}", thumbnailKey);
            log.info("=== 썸네일 파일 존재 확인 완료 (파일 없음) ===");
            return false;
        } catch (S3Exception e) {
            log.error("S3 썸네일 파일 존재 확인 실패: {} - {}", thumbnailKey, e.getMessage());
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
            log.info("비디오 파일 존재 확인: {}", videoKey);
            return true;
            
        } catch (NoSuchKeyException e) {
            log.warn("비디오 파일이 존재하지 않음: {}", videoKey);
            return false;
        } catch (S3Exception e) {
            log.error("S3 파일 존재 확인 실패: {} - {}", videoKey, e.getMessage());
            return false;
        }
    }

    /**
     * S3에서 썸네일 다운로드를 위한 Pre-signed URL 생성
     * 
     * @param thumbnailKey S3 썸네일 키 (예: "thumbnails/user123/uuid_video.jpg")
     * @return Pre-signed URL 문자열 (1시간 유효), thumbnailKey가 null이면 null 반환
     */
    public String getThumbnailUrl(String thumbnailKey) {
        if (thumbnailKey == null || thumbnailKey.trim().isEmpty()) {
            return null;
        }
        
        try {
            log.info("=== 썸네일 Pre-signed URL 생성 시작 ===");
            log.info("입력 파라미터 - 버킷: {}, 키: {}", bucket, thumbnailKey);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(thumbnailKey)
                    .build();
            log.info("GetObjectRequest 생성 완료: bucket={}, key={}", bucket, thumbnailKey);

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))  // 1시간 유효
                    .getObjectRequest(getObjectRequest)
                    .build();
            log.info("GetObjectPresignRequest 생성 완료: 유효시간=1시간");

            log.info("presigner.presignGetObject() 호출 시작...");
            URL presignedUrl = presigner.presignGetObject(presignRequest).url();
            String urlString = presignedUrl.toString();
            
            log.info("썸네일 Pre-signed URL 생성 성공!");
            log.info("생성된 URL (처음 150자): {}", urlString.substring(0, Math.min(150, urlString.length())));
            log.info("=== 썸네일 Pre-signed URL 생성 완료 ===");
            return urlString;
            
        } catch (S3Exception e) {
            log.error("썸네일 Pre-signed URL 생성 실패 (S3 에러): 버킷={}, 키={}, 에러코드={}, 메시지={}", 
                     bucket, thumbnailKey, e.awsErrorDetails().errorCode(), e.getMessage());
            return null; // 프론트엔드에서 에러 처리하도록 null 반환
        } catch (Exception e) {
            log.error("썸네일 Pre-signed URL 생성 실패 (예상치 못한 에러): 버킷={}, 키={}, 에러={}", 
                     bucket, thumbnailKey, e.getMessage(), e);
            return null; // 프론트엔드에서 에러 처리하도록 null 반환
        }
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
                        log.info("extraJson 파싱 실패: {}", e.getMessage());
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
     * 동시성 제어를 위한 락 적용
     */
    private String getSummaryRaw(String videoKey) {
        log.info("=== Summary 파일 읽기 시작 ===");
        log.info("입력 파라미터 - 비디오 키: {}", videoKey);
        
        // videoKey별 Summary 락 획득
        ReentrantLock lock = summaryLocks.computeIfAbsent(videoKey, k -> new ReentrantLock());
        log.info("Summary 락 획득: {}", videoKey);
        
        lock.lock();
        try {
            String originalName = videoKey.split("_", 2)[1];
            // 확장자 제거 (모든 비디오 포맷 지원)
            int lastDotIndex = originalName.lastIndexOf('.');
            String baseName;
            if (lastDotIndex > 0) {
                baseName = originalName.substring(0, lastDotIndex);
            } else {
                baseName = originalName;
            }
            String key = "summary/summary_" + baseName + ".json";
            log.info("Summary 파일 키 생성: {}", key);

            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            log.info("GetObjectRequest 생성 완료: bucket={}, key={}", bucket, key);

            try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(req)) {
                String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                log.info("Summary 파일 읽기 성공: 파일 크기={} bytes", result.length());
                log.info("=== Summary 파일 읽기 완료 ===");
                return result;
            }
        } catch (NoSuchKeyException e) {
            log.info("Summary 파일이 존재하지 않음: {}", videoKey);
            log.info("=== Summary 파일 읽기 완료 (파일 없음) ===");
            return null;
        } catch (S3Exception e) {
            log.error("Summary 파일 읽기 실패 (S3 에러): 버킷={}, 키={}, 에러코드={}, 메시지={}", 
                     bucket, videoKey, e.awsErrorDetails().errorCode(), e.getMessage());
            log.info("=== Summary 파일 읽기 완료 (에러) ===");
            return null;
        } catch (Exception e) {
            log.error("Summary 파일 읽기 실패 (예상치 못한 에러): 비디오키={}, 에러={}", videoKey, e.getMessage(), e);
            log.info("=== Summary 파일 읽기 완료 (에러) ===");
            return null;
        } finally {
            lock.unlock();
            // 안전한 락 정리: 현재 스레드가 보유한 락만 제거
            summaryLocks.computeIfPresent(videoKey, (key, existingLock) -> {
                if (existingLock == lock && !lock.hasQueuedThreads()) {
                    return null; // 제거
                }
                return existingLock; // 유지
            });
        }
    }
}