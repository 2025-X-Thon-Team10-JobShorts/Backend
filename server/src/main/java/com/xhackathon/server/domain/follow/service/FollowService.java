package com.xhackathon.server.domain.follow.service;

import com.xhackathon.server.domain.follow.dto.response.FollowResponse;
import com.xhackathon.server.domain.follow.entity.Follow;
import com.xhackathon.server.domain.follow.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;

    @Transactional
    public FollowResponse toggleFollow(String followerPid, String followeePid) {
        return followRepository.findByFollowerPidAndFolloweePid(followerPid, followeePid)
                .map(existing -> {// 있으면 -> 언팔로우 (삭제)
                    followRepository.delete(existing);
                    return new FollowResponse(
                            null,
                            false,
                            "언팔로우 되었습니다."
                    );
                })
                .orElseGet(() -> {// 없으면 -> 새로 팔로우
                    Follow newFollow = new Follow(followerPid, followeePid);
                    Follow saved = followRepository.save(newFollow);
                    return new FollowResponse(
                            saved.getId(),
                            true,
                            "팔로우 되었습니다."
                    );
                });
    }

    @Transactional(readOnly = true)
    public List<String> getFollowings(String pid) {
        return followRepository.findByFollowerPid(pid)
                .stream()
                .map(Follow::getFolloweePid)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getFollowers(String pid) {
        return followRepository.findByFolloweePid(pid)
                .stream()
                .map(Follow::getFollowerPid)
                .toList();
    }

}
