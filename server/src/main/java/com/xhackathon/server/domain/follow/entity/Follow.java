package com.xhackathon.server.domain.follow.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "follows")
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_pid", nullable = false)
    private String followerPid;

    @Column(name = "followee_pid", nullable = false)
    private String followeePid;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Follow() {}

    public Follow(String followerPid, String followeePid) {
        this.followerPid = followerPid;
        this.followeePid = followeePid;
        this.createdAt = OffsetDateTime.now();
    }

}
