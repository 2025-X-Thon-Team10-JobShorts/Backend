package com.xhackathon.server.domain.mypage.controller;

import com.xhackathon.server.domain.mypage.dto.request.MypageRequest;
import com.xhackathon.server.domain.mypage.dto.request.MypageUpdateRequest;
import com.xhackathon.server.domain.mypage.dto.response.MypageResponse;
import com.xhackathon.server.domain.mypage.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {

    private final MypageService mypageService;

    @PostMapping("/info")
    public ResponseEntity<MypageResponse> getMyPage(@RequestBody MypageRequest request) {
        return ResponseEntity.ok(mypageService.getMyPage(request.getPid()));
    }

    @PatchMapping("/update")
    public ResponseEntity<MypageResponse> updateMyPage(@RequestBody MypageUpdateRequest request) {
        return ResponseEntity.ok(mypageService.updateMyPage(request));
    }
}
