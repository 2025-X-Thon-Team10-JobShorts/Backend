package com.xhackathon.server.domain.follow.controller;

import com.xhackathon.server.domain.follow.dto.request.FollowListRequest;
import com.xhackathon.server.domain.follow.dto.request.FollowRequest;
import com.xhackathon.server.domain.follow.dto.response.FollowResponse;
import com.xhackathon.server.domain.follow.service.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow")
    public ResponseEntity<FollowResponse> toggleFollow(
            @RequestBody FollowRequest request) {
        FollowResponse response =
                followService.toggleFollow(request.getFollowerPid(), request.getFolloweePid());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/followings")
    public ResponseEntity<List<String>> getFollowings(
            @RequestBody FollowListRequest request) {
        List<String> followings = followService.getFollowings(request.getPid());
        return ResponseEntity.ok(followings);
    }

    @GetMapping("/followers")
    public ResponseEntity<List<String>> getFollowers(
            @RequestBody FollowListRequest request) {
        List<String> followers = followService.getFollowers(request.getPid());
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/followers/count")
    public ResponseEntity<Long> getFollowerCount(
            @RequestBody FollowListRequest request) {
        long count = followService.getFollowerCount(request.getPid());
        return ResponseEntity.ok(count);
    }

    @GetMapping("/followings/count")
    public ResponseEntity<Long> getFollowingCount(
            @RequestBody FollowListRequest request) {
        long count = followService.getFollowingCount(request.getPid());
        return ResponseEntity.ok(count);
    }
}
