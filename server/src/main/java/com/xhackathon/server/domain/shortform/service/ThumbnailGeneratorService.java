package com.xhackathon.server.domain.shortform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailGeneratorService {
    
    private final S3Client s3Client;
    
    // 썸네일 크기 설정
    private static final int THUMBNAIL_WIDTH = 320;
    private static final int THUMBNAIL_HEIGHT = 180;
    private static final double FRAME_POSITION_SECONDS = 1.0; // 1초 지점에서 프레임 추출
    private static final int MAX_RETRY_ATTEMPTS = 3; // 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 2000; // 재시도 간격 (2초)
    private static final long PARTIAL_DOWNLOAD_SIZE = 10 * 1024 * 1024; // 처음 10MB만 다운로드 (1초 분량 충분)
    
    /**
     * S3 비디오 파일에서 썸네일을 생성하고 S3에 업로드 (재시도 포함)
     * 
     * @param bucket S3 버킷명
     * @param videoKey 비디오 파일 S3 키
     * @param thumbnailKey 썸네일 파일 S3 키
     * @return 성공 여부
     */
    public boolean generateThumbnailFromS3Video(String bucket, String videoKey, String thumbnailKey) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.info("썸네일 생성 시도 {}/{}: {} -> {}", attempt, MAX_RETRY_ATTEMPTS, videoKey, thumbnailKey);
                
                boolean success = doGenerateThumbnailFromS3Video(bucket, videoKey, thumbnailKey);
                
                if (success) {
                    log.info("썸네일 생성 성공 (시도 {}): {} -> {}", attempt, videoKey, thumbnailKey);
                    return true;
                } else {
                    log.warn("썸네일 생성 실패 (시도 {}): {}", attempt, videoKey);
                }
                
            } catch (Exception e) {
                log.error("썸네일 생성 중 오류 (시도 {}): {} - {}", attempt, videoKey, e.getMessage(), e);
            }
            
            // 마지막 시도가 아니면 잠시 대기
            if (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("재시도 대기 중 인터럽트: {}", ie.getMessage());
                    break;
                }
            }
        }
        
        log.error("썸네일 생성 최종 실패 ({}회 시도): {}", MAX_RETRY_ATTEMPTS, videoKey);
        return false;
    }
    
    /**
     * 실제 썸네일 생성 로직 (단일 시도)
     */
    private boolean doGenerateThumbnailFromS3Video(String bucket, String videoKey, String thumbnailKey) {
        Path tempVideoFile = null;
        Path tempThumbnailFile = null;
        
        try {
            
            // 1. S3에서 비디오 파일을 임시 파일로 다운로드
            tempVideoFile = downloadVideoFromS3(bucket, videoKey);
            if (tempVideoFile == null) {
                log.error("비디오 파일 다운로드 실패: {}", videoKey);
                return false;
            }
            
            // 2. FFmpeg로 썸네일 생성
            tempThumbnailFile = generateThumbnailFromVideo(tempVideoFile);
            if (tempThumbnailFile == null) {
                log.error("썸네일 생성 실패: {}", videoKey);
                return false;
            }
            
            // 3. 생성된 썸네일을 S3에 업로드
            boolean uploadSuccess = uploadThumbnailToS3(bucket, thumbnailKey, tempThumbnailFile);
            
            return uploadSuccess;
            
        } catch (Exception e) {
            log.error("썸네일 생성 중 오류 발생: {} - {}", videoKey, e.getMessage(), e);
            return false;
        } finally {
            // 임시 파일들 정리
            cleanupTempFiles(tempVideoFile, tempThumbnailFile);
        }
    }
    
    /**
     * 비디오 파일 확장자 추출
     */
    private String getVideoExtension(String videoKey) {
        int lastDotIndex = videoKey.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < videoKey.length() - 1) {
            return videoKey.substring(lastDotIndex);
        }
        // 확장자가 없으면 기본값으로 .mp4 사용
        return ".mp4";
    }
    
    /**
     * S3에서 비디오 파일의 처음 부분만 다운로드 (Range 요청 사용)
     * 처음 10MB만 다운로드하여 네트워크 트래픽과 시간을 절약
     */
    private Path downloadVideoFromS3(String bucket, String videoKey) {
        try {
            // Range 요청: 처음 PARTIAL_DOWNLOAD_SIZE 바이트만 다운로드
            String range = "bytes=0-" + (PARTIAL_DOWNLOAD_SIZE - 1);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(videoKey)
                    .range(range)  // Range 헤더 추가
                    .build();
            
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            
            // 원본 파일 확장자 유지하여 임시 파일 생성
            String extension = getVideoExtension(videoKey);
            Path tempFile = Files.createTempFile("video_partial_", extension);
            Files.copy(s3Object, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            long fileSize = Files.size(tempFile);
            log.debug("비디오 파일 부분 다운로드 완료: {} -> {} ({} bytes)", videoKey, tempFile, fileSize);
            return tempFile;
            
        } catch (S3Exception | IOException e) {
            log.error("S3에서 비디오 파일 부분 다운로드 실패: {} - {}", videoKey, e.getMessage());
            // Range 요청이 실패하면 전체 다운로드로 폴백
            log.info("Range 요청 실패, 전체 다운로드로 폴백: {}", videoKey);
            return downloadFullVideoFromS3(bucket, videoKey);
        }
    }
    
    /**
     * S3에서 전체 비디오 파일 다운로드 (폴백용)
     */
    private Path downloadFullVideoFromS3(String bucket, String videoKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(videoKey)
                    .build();
            
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            
            // 원본 파일 확장자 유지하여 임시 파일 생성
            String extension = getVideoExtension(videoKey);
            Path tempFile = Files.createTempFile("video_full_", extension);
            Files.copy(s3Object, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            log.debug("비디오 파일 전체 다운로드 완료: {} -> {}", videoKey, tempFile);
            return tempFile;
            
        } catch (S3Exception | IOException e) {
            log.error("S3에서 비디오 파일 전체 다운로드 실패: {} - {}", videoKey, e.getMessage());
            return null;
        }
    }
    
    /**
     * FFmpeg를 사용하여 비디오에서 썸네일 생성
     */
    private Path generateThumbnailFromVideo(Path videoFile) {
        FFmpegFrameGrabber grabber = null;
        try {
            // FFmpeg 프레임 그래버 생성
            grabber = new FFmpegFrameGrabber(videoFile.toFile());
            grabber.start();
            
            // 비디오 정보 로그
            double duration = grabber.getLengthInTime() / 1_000_000.0; // 마이크로초를 초로 변환
            log.debug("비디오 정보 - 길이: {}초, 프레임레이트: {}", duration, grabber.getFrameRate());
            
            // 1초 지점으로 이동 (또는 비디오 길이의 1/10 지점)
            double seekTime = Math.min(FRAME_POSITION_SECONDS, duration * 0.1);
            grabber.setTimestamp((long)(seekTime * 1_000_000)); // 마이크로초로 변환
            
            // 프레임 추출
            Frame frame = grabber.grabImage();
            if (frame == null) {
                log.error("프레임 추출 실패");
                return null;
            }
            
            // Frame을 BufferedImage로 변환
            BufferedImage image;
            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                image = converter.convert(frame);
                if (image == null) {
                    log.error("이미지 변환 실패");
                    return null;
                }
            }
            
            // 썸네일 크기로 리사이즈
            BufferedImage thumbnail = resizeImage(image, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            
            // 임시 파일로 저장
            Path thumbnailFile = Files.createTempFile("thumbnail_", ".jpg");
            ImageIO.write(thumbnail, "jpg", thumbnailFile.toFile());
            
            log.debug("썸네일 생성 완료: {}", thumbnailFile);
            return thumbnailFile;
            
        } catch (Exception e) {
            log.error("FFmpeg 썸네일 생성 실패: {}", e.getMessage(), e);
            return null;
        } finally {
            if (grabber != null) {
                try {
                    grabber.stop();
                    grabber.release();
                } catch (Exception e) {
                    log.warn("FFmpeg grabber 해제 실패: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 이미지 리사이즈 (고품질)
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // 고품질 리사이즈 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        
        return resizedImage;
    }
    
    /**
     * 생성된 썸네일을 S3에 업로드
     */
    private boolean uploadThumbnailToS3(String bucket, String thumbnailKey, Path thumbnailFile) {
        try {
            byte[] thumbnailData = Files.readAllBytes(thumbnailFile);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(thumbnailKey)
                    .contentType("image/jpeg")
                    .contentLength((long) thumbnailData.length)
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(thumbnailData));
            
            log.debug("썸네일 S3 업로드 완료: {}", thumbnailKey);
            return true;
            
        } catch (S3Exception | IOException e) {
            log.error("썸네일 S3 업로드 실패: {} - {}", thumbnailKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 임시 파일들 정리
     */
    private void cleanupTempFiles(Path... files) {
        for (Path file : files) {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                    log.debug("임시 파일 삭제: {}", file);
                } catch (IOException e) {
                    log.warn("임시 파일 삭제 실패: {} - {}", file, e.getMessage());
                }
            }
        }
    }
}