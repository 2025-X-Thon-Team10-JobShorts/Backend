package com.xhackathon.server.domain.message.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "messages", schema = "public")
@NoArgsConstructor
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;

    @Column(name = "sender_pid", nullable = false)
    private String senderPid;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    public Message(Thread thread, String senderPid, String content) {
        this.thread = thread;
        this.senderPid = senderPid;
        this.content = content;
        this.createdAt = OffsetDateTime.now();
    }

    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = OffsetDateTime.now();
        }
    }
}