package com.xhackathon.server.domain.shortform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Presigner presigner;
    private final S3Client s3Client;

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
}