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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Base64;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;

@Service
@RequiredArgsConstructor
public class ShortFormService {

    private final ShortFormRepository shortFormRepository;
    private final AwsS3Service awsS3Service;
    private final ShortFormAiRepository shortFormAiRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public ShortFormUploadUrlResponse createUploadUrl(ShortFormUploadUrlRequest req) {
        String videoKey = awsS3Service.generateVideoKey(req.getFileName());
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

        return ShortFormResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ShortFormDetailResponse getDetail(Long shortFormId) {

        ShortForm sf = shortFormRepository.findById(shortFormId)
                .orElseThrow(() -> new IllegalArgumentException("ShortForm not found"));

        String summary = awsS3Service.getSummary(shortFormId, sf.getVideoKey());

        return ShortFormDetailResponse.of(sf, summary);
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
            
            // TODO: 실제 STT 및 AI 요약 처리
            // 1. 비디오에서 음성 추출
            // 2. STT API 호출
            // 3. AI 요약 API 호출
            // 4. 결과를 ShortFormAi에 저장
            
            // 임시로 처리 완료 시뮬레이션 (실제로는 외부 API 콜백에서 처리)
            simulateAiProcessingComplete(shortForm.getId());
            
        } catch (Exception e) {
            System.err.println("AI 처리 시작 실패: " + shortForm.getVideoKey() + " - " + e.getMessage());
            
            // 에러 상태로 업데이트
            shortForm.updateStatus(ShortFormStatus.FAILED);
            shortFormRepository.save(shortForm);
        }
    }
    
    private void simulateAiProcessingComplete(Long shortFormId) {
        // 실제 구현에서는 이 메서드가 외부 API 콜백으로 호출됩니다
        try {
            Thread.sleep(5000); // 5초 대기 (실제 처리 시간 시뮬레이션)
            
            ShortFormAi aiRecord = shortFormAiRepository.findByShortFormId(shortFormId)
                    .orElseThrow(() -> new IllegalArgumentException("AI record not found"));
            
            // 가짜 결과로 업데이트
            aiRecord.updateSuccess(
                    "안녕하세요, 저는 개발자입니다. 이 영상에서는 제 경험을 공유하고 싶습니다.",
                    "개발자 자기소개 영상입니다.",
                    "{\"keywords\": [\"개발자\", \"자기소개\", \"경험\"]}"
            );
            shortFormAiRepository.save(aiRecord);
            
            // 숏폼 상태를 완료로 변경
            ShortForm shortForm = shortFormRepository.findById(shortFormId)
                    .orElseThrow(() -> new IllegalArgumentException("ShortForm not found"));
            shortForm.updateStatus(ShortFormStatus.READY_WITH_AI);
            shortFormRepository.save(shortForm);
            
        } catch (Exception e) {
            System.err.println("AI 처리 완료 시뮬레이션 실패: " + e.getMessage());
        }
    }
}
