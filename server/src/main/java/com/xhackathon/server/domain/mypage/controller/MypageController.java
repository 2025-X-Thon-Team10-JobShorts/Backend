package com.xhackathon.server.domain.mypage.controller;

import com.xhackathon.server.domain.mypage.dto.request.MypageRequest;
import com.xhackathon.server.domain.mypage.dto.request.MypageUpdateRequest;
import com.xhackathon.server.domain.mypage.dto.request.ProfileImageUploadUrlRequest;
import com.xhackathon.server.domain.mypage.dto.request.SaveProfileImageRequest;
import com.xhackathon.server.domain.mypage.dto.response.MypageResponse;
import com.xhackathon.server.domain.mypage.dto.response.ProfileImageUploadUrlResponse;
import com.xhackathon.server.domain.mypage.service.MypageService;
import com.xhackathon.server.domain.mypage.service.ProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {

    private final MypageService mypageService;
    private final ProfileImageService profileImageService;

    @PostMapping("/info")
    public ResponseEntity<MypageResponse> getMyPage(@RequestBody MypageRequest request) {
        return ResponseEntity.ok(mypageService.getMyPage(request.getPid()));
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
}
