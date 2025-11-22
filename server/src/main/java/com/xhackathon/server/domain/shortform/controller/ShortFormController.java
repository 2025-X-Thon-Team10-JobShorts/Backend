package com.xhackathon.server.domain.shortform.controller;

import com.xhackathon.server.domain.shortform.dto.request.ShortFormCreateRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormUploadUrlRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormReelsRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormFeedRequest;
import com.xhackathon.server.domain.shortform.dto.request.ShortFormSearchRequest;
import com.xhackathon.server.domain.shortform.dto.response.*;
import com.xhackathon.server.domain.shortform.service.ShortFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/short-forms")
public class ShortFormController {

    private final ShortFormService shortFormService;

    @PostMapping("/upload-url")
    public ResponseEntity<ShortFormUploadUrlResponse> getUploadUrl(
            @RequestBody ShortFormUploadUrlRequest request
    ) {
        return ResponseEntity.ok(shortFormService.createUploadUrl(request));
    }

    @PostMapping
    public ResponseEntity<ShortFormResponse> createShortForm(
            @RequestBody ShortFormCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shortFormService.createShortForm(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShortFormDetailResponse> getShortFormDetail(@PathVariable Long id) {
        return ResponseEntity.ok(shortFormService.getDetail(id));
    }

    @PostMapping("/api/{id}")
    public ResponseEntity<ShortFormReelsResponse> getShortFormReels(
            @PathVariable Long id,
            @RequestBody ShortFormReelsRequest request
    ) {
        return ResponseEntity.ok(shortFormService.getReelsDetail(id, request.getCurrentUserPid()));
    }

    @PostMapping("/api/feed")
    public ResponseEntity<ShortFormFeedResponse> getShortFormFeed(
            @RequestBody ShortFormFeedRequest request
    ) {
        return ResponseEntity.ok(shortFormService.getFeed(request.getPageParam(), request.getSize(), request.getCurrentUserPid()));
    }

    @PostMapping("/api/search")
    public ResponseEntity<ShortFormFeedResponse> searchShortFormsByTag(
            @RequestBody ShortFormSearchRequest request
    ) {
        return ResponseEntity.ok(shortFormService.searchByTag(request.getTag(), request.getPageParam(), request.getSize(), request.getCurrentUserPid()));
    }
}
