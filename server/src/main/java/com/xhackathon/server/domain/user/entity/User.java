package com.xhackathon.server.domain.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Getter
@Table(name = "users", schema = "public")
@NoArgsConstructor
public class User {
    
    @Id
    @Column(name = "pid", nullable = false)
    private String pid;

    @Column(name = "login_id", nullable = false, unique = true, length = 255)
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    private UserRole role;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    private String bio;

    @Column(name = "portfolio_link")
    private String portfolioLink;

    @Column(name = "portfolio_file_url")
    private String portfolioFileUrl;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//    }

    public User(String pid, String loginId, String password, UserRole role, String displayName) {
        this.pid = pid;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
        this.displayName = displayName;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();

    }

    public void updateProfile(String bio,
                              String portfolioLink,
                              String portfolioFileUrl) {

        if (bio != null)
            this.bio = bio;

        if (portfolioLink != null)
            this.portfolioLink = portfolioLink;

        if (portfolioFileUrl != null)
            this.portfolioFileUrl = portfolioFileUrl;

        this.updatedAt = OffsetDateTime.now();
    }

}