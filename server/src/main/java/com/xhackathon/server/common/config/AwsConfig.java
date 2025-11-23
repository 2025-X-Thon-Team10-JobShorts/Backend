package com.xhackathon.server.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
public class AwsConfig {

    @Value("${cloud.aws.region:ap-northeast-2}")
    private String region;

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Bean
    public S3Presigner s3Presigner() {
        log.info("=== AwsConfig: S3Presigner Bean 생성 시작 ===");
        Region awsRegion = Region.of(region);
        
        // 우선순위 1: aws.access-key와 aws.secret-key 설정값이 있으면 사용
        if (accessKey != null && !accessKey.isEmpty() && !accessKey.equals("local-access-key") &&
            secretKey != null && !secretKey.isEmpty() && !secretKey.equals("local-secret-key")) {
            
            log.info("AwsConfig: 설정값 자격 증명 사용 (AccessKey 앞 4자리: {}...)", 
                     accessKey.length() > 4 ? accessKey.substring(0, 4) : "****");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            S3Presigner presigner = S3Presigner.builder()
                    .region(awsRegion)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
            
            log.info("✅ AwsConfig: S3Presigner Bean 생성 완료 (설정값 자격 증명 사용)");
            return presigner;
        }
        
        // 우선순위 2: 환경변수 AWS_ACCESS_KEY_ID와 AWS_SECRET_ACCESS_KEY에서 직접 가져와서 사용
        String envAccessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String envSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        
        if (envAccessKey != null && !envAccessKey.isEmpty() && 
            envSecretKey != null && !envSecretKey.isEmpty()) {
            
            log.info("AwsConfig: 환경변수 자격 증명 사용 (AccessKey 앞 4자리: {}...)", 
                     envAccessKey.length() > 4 ? envAccessKey.substring(0, 4) : "****");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(envAccessKey, envSecretKey);
            
            S3Presigner presigner = S3Presigner.builder()
                    .region(awsRegion)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
            
            log.info("✅ AwsConfig: S3Presigner Bean 생성 완료 (환경변수 자격 증명 사용)");
            return presigner;
        }
        
        // 실패시: 자격증명이 없으면 IllegalStateException 발생
        log.error("❌ AwsConfig: AWS 자격 증명을 찾을 수 없습니다.");
        log.error("설정 방법:");
        log.error("  1. application.properties에 aws.access-key와 aws.secret-key 설정");
        log.error("  2. 환경변수 AWS_ACCESS_KEY_ID와 AWS_SECRET_ACCESS_KEY 설정");
        throw new IllegalStateException("AWS 자격 증명이 설정되지 않았습니다. " +
                "application.properties의 aws.access-key/aws.secret-key 또는 " +
                "환경변수 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY를 설정해주세요.");
    }

    @Bean
    public S3Client s3Client() {
        log.info("=== AwsConfig: S3Client Bean 생성 시작 ===");
        Region awsRegion = Region.of(region);
        
        // 우선순위 1: aws.access-key와 aws.secret-key 설정값이 있으면 사용
        if (accessKey != null && !accessKey.isEmpty() && !accessKey.equals("local-access-key") &&
            secretKey != null && !secretKey.isEmpty() && !secretKey.equals("local-secret-key")) {
            
            log.info("AwsConfig: 설정값 자격 증명 사용 (AccessKey 앞 4자리: {}...)", 
                     accessKey.length() > 4 ? accessKey.substring(0, 4) : "****");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            S3Client client = S3Client.builder()
                    .region(awsRegion)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
            
            log.info("✅ AwsConfig: S3Client Bean 생성 완료 (설정값 자격 증명 사용)");
            return client;
        }
        
        // 우선순위 2: 환경변수 AWS_ACCESS_KEY_ID와 AWS_SECRET_ACCESS_KEY에서 직접 가져와서 사용
        String envAccessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String envSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        
        if (envAccessKey != null && !envAccessKey.isEmpty() && 
            envSecretKey != null && !envSecretKey.isEmpty()) {
            
            log.info("AwsConfig: 환경변수 자격 증명 사용 (AccessKey 앞 4자리: {}...)", 
                     envAccessKey.length() > 4 ? envAccessKey.substring(0, 4) : "****");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(envAccessKey, envSecretKey);
            
            S3Client client = S3Client.builder()
                    .region(awsRegion)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
            
            log.info("✅ AwsConfig: S3Client Bean 생성 완료 (환경변수 자격 증명 사용)");
            return client;
        }
        
        // 실패시: 자격증명이 없으면 IllegalStateException 발생
        log.error("❌ AwsConfig: AWS 자격 증명을 찾을 수 없습니다.");
        log.error("설정 방법:");
        log.error("  1. application.properties에 aws.access-key와 aws.secret-key 설정");
        log.error("  2. 환경변수 AWS_ACCESS_KEY_ID와 AWS_SECRET_ACCESS_KEY 설정");
        throw new IllegalStateException("AWS 자격 증명이 설정되지 않았습니다. " +
                "application.properties의 aws.access-key/aws.secret-key 또는 " +
                "환경변수 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY를 설정해주세요.");
    }
}
