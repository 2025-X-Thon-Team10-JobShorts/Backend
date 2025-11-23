package com.xhackathon.server.domain.shortform.service;

import com.xhackathon.server.domain.shortform.dto.request.ShortFormCreateRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormUploadUrlRequest;
import com.xhackathon.server.domain.shortform.dto.response.*;
import com.xhackathon.server.domain.shortform.entity.ShortFormAiStatus;
import com.xhackathon.server.domain.shortform.entity.ShortFormStatus;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormAi;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.shortform.repository.ShortFormAiRepository;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.repository.UserRepository;
import com.xhackathon.server.domain.follow.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Base64;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortFormService {

    private final ShortFormRepository shortFormRepository;
    private final AwsS3Service awsS3Service;
    private final ShortFormAiRepository shortFormAiRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final S3CrawlingService s3CrawlingService;

    @Transactional(readOnly = true)
    public ShortFormUploadUrlResponse createUploadUrl(ShortFormUploadUrlRequest req) {
        String videoKey = awsS3Service.generateVideoKey(
                req.getOwnerPid(),
                req.getFileName());
        URL uploadUrl = awsS3Service.generatePresignedUploadUrl(videoKey, req.getMimeType());

        return new ShortFormUploadUrlResponse(uploadUrl.toString(), videoKey);
    }

    @Transactional
    public ShortFormResponse createShortForm(ShortFormCreateRequest req) {

        ShortForm shortForm = new ShortForm(
                req.getOwnerPid(),
                req.getTitle(),
                req.getDescription(),
                req.getVideoKey(),
                req.getDurationSec(),
                req.getTags()
        );

        ShortForm saved = shortFormRepository.save(shortForm);

        // 썸네일 생성 비동기 처리
        generateThumbnailAsync(saved);
        
        // AI 처리 시작
        startAiProcessingAsync(saved);
        
        // S3에 summary 파일이 있으면 태그 정보 업데이트
        updateTagsFromSummaryAsync(saved);

        return ShortFormResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ShortFormDetailResponse getDetail(Long shortFormId) {

        ShortForm sf = shortFormRepository.findById(shortFormId)
                .orElseThrow(() -> new IllegalArgumentException("ShortForm not found"));

        try {
            // Summary에서 상세 정보 추출
            String summary = awsS3Service.getSummaryFromSummary(sf.getVideoKey());
            String transcript = awsS3Service.getTranscriptFromSummary(sf.getVideoKey());
            
            // 비디오 및 썸네일 URL 생성
            String videoUrl = awsS3Service.generateVideoUrl(sf.getVideoKey());
            String thumbnailUrl = awsS3Service.getThumbnailUrl(sf.getThumbnailKey());

            return ShortFormDetailResponse.of(sf, summary, transcript, videoUrl, thumbnailUrl);
            
        } catch (Exception e) {
            log.warn("Summary 파일 읽기 실패, 기본 정보만 반환: {} - {}", sf.getVideoKey(), e.getMessage());
            
            // Summary 파일이 없거나 읽을 수 없는 경우 기본 정보만 반환
            String videoUrl = awsS3Service.generateVideoUrl(sf.getVideoKey());
            String thumbnailUrl = awsS3Service.getThumbnailUrl(sf.getThumbnailKey());
            
            return ShortFormDetailResponse.of(sf, "", "", videoUrl, thumbnailUrl);
        }
    }

    @Transactional(readOnly = true)
    public ShortFormReelsResponse getReelsDetail(Long shortFormId) {
        return getReelsDetail(shortFormId, null);
    }
    
    @Transactional(readOnly = true)
    public ShortFormReelsResponse getReelsDetail(Long shortFormId, String currentUserPid) {
        ShortForm sf = shortFormRepository.findById(shortFormId)
                .orElseThrow(() -> new IllegalArgumentException("ShortForm not found"));

        OwnerInfo owner = getOwnerInfo(sf.getOwnerPid(), currentUserPid);
        
        // 비디오 URL 및 썸네일 URL 생성
        String videoUrl = awsS3Service.generateVideoUrl(sf.getVideoKey());
        String thumbnailUrl = awsS3Service.getThumbnailUrl(sf.getThumbnailKey());
        
        // AI 처리 상태 조회
        Optional<ShortFormAi> aiOpt = shortFormAiRepository.findByShortFormId(shortFormId);
        String summary = aiOpt.map(ShortFormAi::getSummary).orElse("");
        ShortFormAiStatus aiStatus = aiOpt.map(ShortFormAi::getStatus).orElse(ShortFormAiStatus.PENDING);

        return ShortFormReelsResponse.of(sf, owner, videoUrl, thumbnailUrl, summary, aiStatus);
    }

    @Transactional(readOnly = true)
    public ShortFormFeedResponse getFeed(String pageParam, int size) {
        return getFeed(pageParam, size, null);
    }
    
    @Transactional(readOnly = true)
    public ShortFormFeedResponse getFeed(String pageParam, int size, String currentUserPid) {
        Pageable pageable = PageRequest.of(0, size + 1); // 하나 더 가져와서 다음 페이지 존재 확인
        
        List<ShortForm> shortForms;
        if (pageParam == null) {
            // 첫 페이지
            shortForms = shortFormRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            // 커서 기반 페이징
            Long cursorId = decodeCursor(pageParam);
            shortForms = shortFormRepository.findByIdLessThanOrderByCreatedAtDesc(cursorId, pageable);
        }

        boolean hasNextPage = shortForms.size() > size;
        if (hasNextPage) {
            shortForms = shortForms.subList(0, size); // 마지막 항목 제거
        }

        List<ShortFormReelsResponse> data = shortForms.stream()
                .<ShortFormReelsResponse>map(sf -> {
                    OwnerInfo owner = getOwnerInfo(sf.getOwnerPid(), currentUserPid);
                    String videoUrl = awsS3Service.generateVideoUrl(sf.getVideoKey());
                    String thumbnailUrl = awsS3Service.getThumbnailUrl(sf.getThumbnailKey());
                    
                    Optional<ShortFormAi> aiOpt = shortFormAiRepository.findByShortFormId(sf.getId());
                    String summary = aiOpt.map(ShortFormAi::getSummary).orElse("");
                    ShortFormAiStatus aiStatus = aiOpt.map(ShortFormAi::getStatus).orElse(ShortFormAiStatus.PENDING);
                    
                    return ShortFormReelsResponse.of(sf, owner, videoUrl, thumbnailUrl, summary, aiStatus);
                })
                .toList();

        String nextPageParam = hasNextPage ? encodeCursor(shortForms.get(shortForms.size() - 1).getId()) : null;

        return ShortFormFeedResponse.of(data, nextPageParam, hasNextPage);
    }

    @Transactional(readOnly = true)
    public ShortFormFeedResponse searchByTag(String tag, String pageParam, int size, String currentUserPid) {
        Pageable pageable = PageRequest.of(0, size + 1);
        
        List<ShortForm> shortForms;
        if (pageParam == null) {
            shortForms = shortFormRepository.findByTagContaining(tag, pageable);
        } else {
            Long cursorId = decodeCursor(pageParam);
            shortForms = shortFormRepository.findByTagContainingAndIdLessThan(tag, cursorId, pageable);
        }

        boolean hasNextPage = shortForms.size() > size;
        if (hasNextPage) {
            shortForms = shortForms.subList(0, size);
        }

        List<ShortFormReelsResponse> data = shortForms.stream()
                .<ShortFormReelsResponse>map(sf -> {
                    OwnerInfo owner = getOwnerInfo(sf.getOwnerPid(), currentUserPid);
                    String videoUrl = awsS3Service.generateVideoUrl(sf.getVideoKey());
                    String thumbnailUrl = awsS3Service.getThumbnailUrl(sf.getThumbnailKey());
                    
                    Optional<ShortFormAi> aiOpt = shortFormAiRepository.findByShortFormId(sf.getId());
                    String summary = aiOpt.map(ShortFormAi::getSummary).orElse("");
                    ShortFormAiStatus aiStatus = aiOpt.map(ShortFormAi::getStatus).orElse(ShortFormAiStatus.PENDING);
                    
                    return ShortFormReelsResponse.of(sf, owner, videoUrl, thumbnailUrl, summary, aiStatus);
                })
                .toList();

        String nextPageParam = hasNextPage ? encodeCursor(shortForms.get(shortForms.size() - 1).getId()) : null;

        return ShortFormFeedResponse.of(data, nextPageParam, hasNextPage);
    }

    private Long decodeCursor(String cursor) {
        try {
            return Long.parseLong(new String(Base64.getDecoder().decode(cursor)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private String encodeCursor(Long id) {
        return Base64.getEncoder().encodeToString(id.toString().getBytes());
    }
    
    private OwnerInfo getOwnerInfo(String ownerPid, String currentUserPid) {
        User owner = userRepository.findByPid(ownerPid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + ownerPid));
        
        // 팔로우 상태 확인
        boolean isFollowed = false;
        if (currentUserPid != null && !currentUserPid.equals(ownerPid)) {
            isFollowed = followRepository.findByFollowerPidAndFolloweePid(currentUserPid, ownerPid)
                    .isPresent();
        }
        
        return OwnerInfo.of(
                Long.valueOf(owner.getPid().hashCode()), // 임시 ID 생성
                owner.getDisplayName(),
                owner.getProfileImageUrl(),
                isFollowed
        );
    }
    
    @Async
    public void generateThumbnailAsync(ShortForm shortForm) {
        String videoKey = shortForm.getVideoKey();
        String thumbnailKey = awsS3Service.generateThumbnailKey(videoKey);
        
        try {
            // 1. 썸네일 실제 생성 (S3 업로드까지 완료)
            boolean thumbnailGenerated = awsS3Service.generateThumbnail(videoKey);
            
            if (thumbnailGenerated) {
                // 2. 썸네일 생성 성공 시에만 DB 업데이트
                updateThumbnailInDatabase(shortForm.getId(), thumbnailKey);
                System.out.println("썸네일 생성 완료: " + videoKey + " -> " + thumbnailKey);
            } else {
                System.err.println("썸네일 생성 실패: " + videoKey);
            }
            
        } catch (Exception e) {
            System.err.println("썸네일 생성 중 오류: " + videoKey + " - " + e.getMessage());
            // 필요시 재시도 로직 추가 가능
        }
    }
    
    @Transactional
    public void updateThumbnailInDatabase(Long shortFormId, String thumbnailKey) {
        ShortForm shortForm = shortFormRepository.findById(shortFormId)
                .orElseThrow(() -> new IllegalArgumentException("ShortForm not found: " + shortFormId));
        
        shortForm.updateThumbnail(thumbnailKey);
        shortFormRepository.save(shortForm);
    }
    
    @Async
    public void startAiProcessingAsync(ShortForm shortForm) {
        try {
            // AI 처리 레코드 생성
            ShortFormAi aiRecord = ShortFormAi.createPending(shortForm.getId());
            shortFormAiRepository.save(aiRecord);
            
            // 숏폼 상태를 STT 처리 중으로 변경
            shortForm.updateStatus(ShortFormStatus.PROCESSING_STT);
            shortFormRepository.save(shortForm);
            
            // AI Pod에 처리 요청 전송
            boolean aiJobStarted = requestAiProcessing(shortForm.getVideoKey());
            
            if (!aiJobStarted) {
                log.warn("AI 처리 요청 실패, 상태를 FAILED로 변경: {}", shortForm.getVideoKey());
                shortForm.updateStatus(ShortFormStatus.FAILED);
                shortFormRepository.save(shortForm);
            }
            
        } catch (Exception e) {
            System.err.println("AI 처리 시작 실패: " + shortForm.getVideoKey() + " - " + e.getMessage());
            
            // 에러 상태로 업데이트
            shortForm.updateStatus(ShortFormStatus.FAILED);
            shortFormRepository.save(shortForm);
        }
    }
    
    /**
     * AI Pod에 처리 요청 전송
     * 실제 구현에서는 HTTP 클라이언트로 AI Pod API 호출
     */
    private boolean requestAiProcessing(String videoKey) {
        try {
            // TODO: 실제 AI Pod API 호출 구현
            // HTTP POST 요청을 AI Pod의 /api/process 엔드포인트로 전송
            // 요청 데이터: { "jobId": videoKey, "s3Key": videoKey, "callbackUrl": "http://backend/internal/jobs/{jobId}/complete" }
            
            log.info("AI 처리 요청 전송: {}", videoKey);
            
            // 임시로 성공 반환 (실제로는 HTTP 응답 상태 확인)
            return true;
            
        } catch (Exception e) {
            log.error("AI 처리 요청 전송 실패: {} - {}", videoKey, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * S3 Summary 파일에서 태그 정보를 비동기로 업데이트
     */
    @Async
    public void updateTagsFromSummaryAsync(ShortForm shortForm) {
        try {
            List<String> tags = awsS3Service.extractTagsFromSummary(shortForm.getVideoKey());
            
            if (!tags.isEmpty()) {
                log.info("Summary에서 태그 추출 성공: {} - 태그 개수: {}", shortForm.getVideoKey(), tags.size());
                
                // DB 업데이트
                updateShortFormTags(shortForm.getId(), tags);
            } else {
                log.debug("Summary 파일에서 태그를 찾을 수 없음: {}", shortForm.getVideoKey());
            }
            
        } catch (Exception e) {
            log.warn("Summary에서 태그 추출 중 오류 (정상적인 경우일 수 있음): {} - {}", 
                     shortForm.getVideoKey(), e.getMessage());
        }
    }
    
    /**
     * ShortForm의 태그 정보 업데이트
     */
    @Transactional
    public void updateShortFormTags(Long shortFormId, List<String> tags) {
        ShortForm shortForm = shortFormRepository.findById(shortFormId)
                .orElseThrow(() -> new IllegalArgumentException("ShortForm not found: " + shortFormId));
        
        shortForm.updateTags(tags);
        shortFormRepository.save(shortForm);
        
        log.info("ShortForm 태그 업데이트 완료: {} - 태그: {}", shortFormId, tags);
    }
}
