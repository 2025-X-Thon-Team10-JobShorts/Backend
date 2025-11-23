package com.xhackathon.server.domain.mypage.controller;

import com.xhackathon.server.domain.mypage.dto.request.MypageRequest;
import com.xhackathon.server.domain.mypage.dto.request.MypageUpdateRequest;
import com.xhackathon.server.domain.mypage.dto.request.ProfileImageUploadUrlRequest;
import com.xhackathon.server.domain.mypage.dto.request.SaveProfileImageRequest;
import com.xhackathon.server.domain.mypage.dto.response.*;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.mypage.service.MypageService;
import com.xhackathon.server.domain.mypage.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {

    private final MypageService mypageService;
    private final ProfileImageService profileImageService;

    @GetMapping("/info")
    public ResponseEntity<MyPageInfoResponse> getMyPageInfo(@RequestParam String pid) {
        return ResponseEntity.ok(mypageService.getMyPageInfo(pid));
    }

    @PatchMapping("/update")
    public ResponseEntity<MypageResponse> updateMyPage(@RequestBody MypageUpdateRequest request) {
        return ResponseEntity.ok(mypageService.updateMyPage(request));
    }

    @PostMapping("/profile-image/upload-url")
    public ResponseEntity<ProfileImageUploadUrlResponse> getProfileImageUploadUrl(
            @RequestBody ProfileImageUploadUrlRequest request) {
        return ResponseEntity.ok(profileImageService.createUploadUrl(request));
    }

    @PostMapping("/profile-image/save")
    public ResponseEntity<String> saveProfileImage(
            @RequestBody SaveProfileImageRequest request) {
        profileImageService.saveProfileImage(request);
        return ResponseEntity.ok("프로필 이미지 업데이트 완료");
    }

    @GetMapping("/posts")
    public ResponseEntity<List<ShortForm>> getMyPosts(@RequestParam String pid) {
        return ResponseEntity.ok(mypageService.getMyPosts(pid));
    }

    @GetMapping("/bookmarked")
    public ResponseEntity<BookmarkedApplicantListResponse> getBookmarkedApplicants(
            @RequestParam String companyPid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(mypageService.getBookmarkedApplicants(companyPid, page, limit));
    }

    @PostMapping("/bookmark/{applicantId}")
    public ResponseEntity<BookmarkToggleResponse> toggleApplicantBookmark(
            @RequestParam String companyPid,
            @PathVariable String applicantId) {
        return ResponseEntity.ok(mypageService.toggleApplicantBookmark(companyPid, applicantId));
    }
}
