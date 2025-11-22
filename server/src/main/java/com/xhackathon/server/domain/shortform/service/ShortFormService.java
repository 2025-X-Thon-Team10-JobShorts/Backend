package com.xhackathon.server.domain.shortform.service;

import com.xhackathon.server.domain.shortform.dto.request.ShortFormCreateRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormUploadUrlRequest;
import com.xhackathon.server.domain.shortform.dto.response.ShortFormDetailResponse;
import com.xhackathon.server.domain.shortform.dto.response.ShortFormResponse;
import com.xhackathon.server.domain.shortform.dto.response.ShortFormUploadUrlResponse;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.shortform.entity.ShortFormAi;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.shortform.repository.ShortFormAiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;

@Service
@RequiredArgsConstructor
public class ShortFormService {

    private final ShortFormRepository shortFormRepository;
    private final AwsS3Service awsS3Service;
    private final ShortFormAiRepository shortFormAiRepository;

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
                req.getDurationSec()
        );

        ShortForm saved = shortFormRepository.save(shortForm);

        return ShortFormResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ShortFormDetailResponse getDetail(Long shortFormId) {

        ShortForm sf = shortFormRepository.findById(shortFormId)
                .orElseThrow(() -> new IllegalArgumentException("ShortForm not found"));

        String summary = awsS3Service.getSummary(shortFormId, sf.getVideoKey());

        return ShortFormDetailResponse.of(sf, summary);
    }
}
