package com.xhackathon.server.domain.shortform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xhackathon.server.domain.shortform.dto.request.AiCallbackRequest;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormAi;
import com.xhackathon.server.domain.shortform.entity.ShortFormStatus;
import com.xhackathon.server.domain.shortform.repository.ShortFormAiRepository;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
     * AI 코드는 flat 구조(transcript, summary 최상위) 또는 nested 구조(result 객체) 모두 지원
     */
    private boolean handleSuccessCallback(ShortForm shortForm, ShortFormAi aiRecord, AiCallbackRequest request) {
        try {
            // transcript와 summary 추출 (flat 구조 우선, 없으면 nested 구조)
            String transcript = request.getTranscript();
            String summary = request.getSummary();
            
            // nested 구조에서 가져오기 (하위 호환성)
            AiCallbackRequest.AiResult result = request.getResult();
            if (transcript == null && result != null) {
                transcript = result.getTranscript();
            }
            if (summary == null && result != null) {
                summary = result.getSummary();
            }
            
            // transcript나 summary가 없으면 에러
            if ((transcript == null || transcript.isEmpty()) && (summary == null || summary.isEmpty())) {
                log.error("SUCCESS 상태이지만 transcript와 summary가 모두 없음: {}", shortForm.getVideoKey());
                return false;
            }
            
            // extraJson 생성 (request와 result 모두 포함)
            String extraJson = createExtraJson(request, result);
            
            // ShortFormAi 업데이트
            aiRecord.updateSuccess(
                    transcript != null ? transcript : "",
                    summary != null ? summary : "",
                    extraJson
            );
            shortFormAiRepository.save(aiRecord);
            
            // extraJson에서 태그 추출하여 ShortForm에 업데이트
            List<String> tags = extractTagsFromExtraJson(extraJson, request, result);
            if (tags != null && !tags.isEmpty()) {
                shortForm.updateTags(tags);
                log.info("태그 업데이트 완료 - shortFormId: {}, tags: {}", shortForm.getId(), tags);
            }
            
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
            // 에러 메시지 처리: error_message > error > 기본 메시지
            String errorMessage = request.getErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = request.getError();
            }
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "AI 처리 실패 (상세 오류 없음)";
            }
            
            // 에러 코드가 있으면 에러 메시지에 포함
            if (request.getErrorCode() != null && !request.getErrorCode().isEmpty()) {
                errorMessage = String.format("[%s] %s", request.getErrorCode(), errorMessage);
            }
            
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
     * AI 코드의 meta 정보도 포함
     */
    private String createExtraJson(AiCallbackRequest request, AiCallbackRequest.AiResult result) {
        try {
            Map<String, Object> extraData = new HashMap<>();
            
            // result가 있을 때만 처리 (nested 구조)
            if (result != null) {
                // 키워드 추가
                if (result.getKeywords() != null && result.getKeywords().length > 0) {
                    extraData.put("keywords", Arrays.asList(result.getKeywords()));
                }
                
                // 처리 시간 추가
                if (result.getProcessingTime() != null) {
                    extraData.put("processingTime", result.getProcessingTime());
                }
            }
            
            // AI 코드의 meta 정보 병합 (duration_ms -> processingTime, model, stt_engine 등)
            if (request.getMeta() != null) {
                extraData.putAll(request.getMeta());
                
                // duration_ms를 processingTime으로도 저장 (일관성)
                if (request.getMeta().containsKey("duration_ms") && !extraData.containsKey("processingTime")) {
                    Object durationMs = request.getMeta().get("duration_ms");
                    if (durationMs instanceof Number) {
                        extraData.put("processingTime", ((Number) durationMs).doubleValue() / 1000.0); // ms -> seconds
                    }
                }
            }
            
            // result_s3_key 추가 (S3에 업로드한 summary JSON 파일 키)
            if (request.getResultS3Key() != null && !request.getResultS3Key().isEmpty()) {
                extraData.put("resultS3Key", request.getResultS3Key());
            }
            
            // s3_bucket, s3_key 추가 (참고용)
            if (request.getS3Bucket() != null && !request.getS3Bucket().isEmpty()) {
                extraData.put("s3Bucket", request.getS3Bucket());
            }
            if (request.getS3Key() != null && !request.getS3Key().isEmpty()) {
                extraData.put("s3Key", request.getS3Key());
            }
            
            extraData.put("processedAt", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(extraData);
            
        } catch (JsonProcessingException e) {
            log.warn("extraJson 생성 실패, 빈 객체로 대체: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * extraJson에서 태그(Keywords) 추출
     * 
     * @param extraJson extraJson 문자열
     * @param request AI 콜백 요청
     * @param result AI 결과 객체
     * @return 태그 목록
     */
    private List<String> extractTagsFromExtraJson(String extraJson, AiCallbackRequest request, AiCallbackRequest.AiResult result) {
        try {
            List<String> tags = new ArrayList<>();
            
            // 1. result 객체에서 keywords 추출 (nested 구조)
            if (result != null && result.getKeywords() != null && result.getKeywords().length > 0) {
                tags.addAll(Arrays.asList(result.getKeywords()));
                log.debug("result에서 태그 추출: {}", tags);
                return tags;
            }
            
            // 2. extraJson 문자열에서 keywords 추출
            if (extraJson != null && !extraJson.trim().isEmpty() && extraJson.startsWith("{")) {
                try {
                    JsonNode extraJsonNode = objectMapper.readTree(extraJson);
                    
                    // keywords 필드 확인
                    if (extraJsonNode.has("keywords") && extraJsonNode.get("keywords").isArray()) {
                        for (JsonNode keywordNode : extraJsonNode.get("keywords")) {
                            if (keywordNode.isTextual()) {
                                tags.add(keywordNode.asText());
                            }
                        }
                        if (!tags.isEmpty()) {
                            log.debug("extraJson에서 keywords 추출: {}", tags);
                            return tags;
                        }
                    }
                    
                    // tags 필드 확인 (대체 필드)
                    if (tags.isEmpty() && extraJsonNode.has("tags") && extraJsonNode.get("tags").isArray()) {
                        for (JsonNode tagNode : extraJsonNode.get("tags")) {
                            if (tagNode.isTextual()) {
                                tags.add(tagNode.asText());
                            }
                        }
                        if (!tags.isEmpty()) {
                            log.debug("extraJson에서 tags 추출: {}", tags);
                            return tags;
                        }
                    }
                } catch (Exception e) {
                    log.debug("extraJson 파싱 실패: {}", e.getMessage());
                }
            }
            
            // 3. request의 meta에서 keywords 확인
            if (tags.isEmpty() && request.getMeta() != null) {
                Object keywordsObj = request.getMeta().get("keywords");
                if (keywordsObj != null) {
                    if (keywordsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> keywordsList = (List<Object>) keywordsObj;
                        for (Object keyword : keywordsList) {
                            if (keyword != null) {
                                tags.add(keyword.toString());
                            }
                        }
                    } else if (keywordsObj instanceof String[]) {
                        tags.addAll(Arrays.asList((String[]) keywordsObj));
                    }
                    if (!tags.isEmpty()) {
                        log.debug("meta에서 keywords 추출: {}", tags);
                        return tags;
                    }
                }
            }
            
            log.debug("태그를 찾을 수 없음");
            return tags;
            
        } catch (Exception e) {
            log.warn("태그 추출 중 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}