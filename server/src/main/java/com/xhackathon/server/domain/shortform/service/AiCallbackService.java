package com.xhackathon.server.domain.shortform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhackathon.server.domain.shortform.dto.request.AiCallbackRequest;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormAi;
import com.xhackathon.server.domain.shortform.entity.ShortFormAiStatus;
import com.xhackathon.server.domain.shortform.entity.ShortFormStatus;
import com.xhackathon.server.domain.shortform.repository.ShortFormAiRepository;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallbackService {

    private final ShortFormRepository shortFormRepository;
    private final ShortFormAiRepository shortFormAiRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI Pod에서 전송된 콜백을 처리
     * 
     * @param videoKey S3 비디오 키 (jobId)
     * @param request AI 처리 결과
     * @return 처리 성공 여부
     */
    @Transactional
    public boolean processAiCallback(String videoKey, AiCallbackRequest request) {
        try {
            log.info("AI 콜백 처리 시작 - videoKey: {}, status: {}", videoKey, request.getStatus());
            
            // 1. videoKey로 ShortForm 조회
            Optional<ShortForm> shortFormOpt = shortFormRepository.findByVideoKey(videoKey);
            if (shortFormOpt.isEmpty()) {
                log.error("해당 videoKey의 ShortForm을 찾을 수 없음: {}", videoKey);
                return false;
            }
            
            ShortForm shortForm = shortFormOpt.get();
            
            // 2. ShortFormAi 레코드 조회 또는 생성
            ShortFormAi aiRecord = shortFormAiRepository.findByShortFormId(shortForm.getId())
                    .orElseGet(() -> {
                        log.info("ShortFormAi 레코드가 없어서 새로 생성: {}", shortForm.getId());
                        return ShortFormAi.createPending(shortForm.getId());
                    });
            
            // 3. 콜백 상태에 따른 처리
            if ("SUCCESS".equals(request.getStatus())) {
                return handleSuccessCallback(shortForm, aiRecord, request);
            } else if ("FAILED".equals(request.getStatus())) {
                return handleFailureCallback(shortForm, aiRecord, request);
            } else {
                log.warn("알 수 없는 콜백 상태: {}", request.getStatus());
                return false;
            }
            
        } catch (Exception e) {
            log.error("AI 콜백 처리 중 오류: {} - {}", videoKey, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 성공 콜백 처리
     */
    private boolean handleSuccessCallback(ShortForm shortForm, ShortFormAi aiRecord, AiCallbackRequest request) {
        try {
            AiCallbackRequest.AiResult result = request.getResult();
            if (result == null) {
                log.error("SUCCESS 상태이지만 result가 null: {}", shortForm.getVideoKey());
                return false;
            }
            
            // extraJson 생성
            String extraJson = createExtraJson(result);
            
            // ShortFormAi 업데이트
            aiRecord.updateSuccess(
                    result.getTranscript(),
                    result.getSummary(),
                    extraJson
            );
            shortFormAiRepository.save(aiRecord);
            
            // ShortForm 상태 업데이트
            shortForm.updateStatus(ShortFormStatus.READY_WITH_AI);
            shortFormRepository.save(shortForm);
            
            log.info("AI 처리 성공 콜백 완료 - shortFormId: {}", shortForm.getId());
            return true;
            
        } catch (Exception e) {
            log.error("성공 콜백 처리 실패: {} - {}", shortForm.getVideoKey(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 실패 콜백 처리
     */
    private boolean handleFailureCallback(ShortForm shortForm, ShortFormAi aiRecord, AiCallbackRequest request) {
        try {
            String errorMessage = request.getError() != null ? request.getError() : "AI 처리 실패 (상세 오류 없음)";
            
            // ShortFormAi 업데이트
            aiRecord.updateError(errorMessage);
            shortFormAiRepository.save(aiRecord);
            
            // ShortForm 상태 업데이트
            shortForm.updateStatus(ShortFormStatus.FAILED);
            shortFormRepository.save(shortForm);
            
            log.warn("AI 처리 실패 콜백 완료 - shortFormId: {}, error: {}", shortForm.getId(), errorMessage);
            return true;
            
        } catch (Exception e) {
            log.error("실패 콜백 처리 실패: {} - {}", shortForm.getVideoKey(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * AI 결과에서 extraJson 생성
     */
    private String createExtraJson(AiCallbackRequest.AiResult result) {
        try {
            Map<String, Object> extraData = new HashMap<>();
            
            if (result.getKeywords() != null) {
                extraData.put("keywords", Arrays.asList(result.getKeywords()));
            }
            
            if (result.getProcessingTime() != null) {
                extraData.put("processingTime", result.getProcessingTime());
            }
            
            extraData.put("processedAt", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(extraData);
            
        } catch (JsonProcessingException e) {
            log.warn("extraJson 생성 실패, 빈 객체로 대체: {}", e.getMessage());
            return "{}";
        }
    }
}