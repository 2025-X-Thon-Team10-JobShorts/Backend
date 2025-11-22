package com.xhackathon.server.domain.message.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "threads", schema = "public")
@NoArgsConstructor
public class Thread {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_1_pid", nullable = false)
    private String participant1Pid;

    @Column(name = "participant_2_pid", nullable = false)
    private String participant2Pid;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();

    public Thread(String participant1Pid, String participant2Pid) {
        this.participant1Pid = participant1Pid;
        this.participant2Pid = participant2Pid;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateLastActivity() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isParticipant(String userPid) {
        return participant1Pid.equals(userPid) || participant2Pid.equals(userPid);
    }

    public String getOtherParticipant(String userPid) {
        if (participant1Pid.equals(userPid)) {
            return participant2Pid;
        } else if (participant2Pid.equals(userPid)) {
            return participant1Pid;
        }
        throw new IllegalArgumentException("User is not a participant of this thread");
    }
}