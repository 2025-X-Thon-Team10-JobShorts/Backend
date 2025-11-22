package com.xhackathon.server.domain.follow.repository;

import com.xhackathon.server.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerPidAndFolloweePid(String followerPid, String followeePid);

    //내 팔로잉 찾기
    List<Follow> findByFollowerPid(String followerPid);

    //내 팔로워 찾기
    List<Follow> findByFolloweePid(String followeePid);

    //팔로워 수 카운트
    long countByFolloweePid(String followeePid);

    //팔로잉 수 카운트
    long countByFollowerPid(String followerPid);
}
