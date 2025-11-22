package com.xhackathon.server.domain.mypage.service;

import com.xhackathon.server.domain.mypage.dto.request.ProfileImageUploadUrlRequest;
import com.xhackathon.server.domain.mypage.dto.request.SaveProfileImageRequest;
import com.xhackathon.server.domain.mypage.dto.response.ProfileImageUploadUrlResponse;
import com.xhackathon.server.domain.shortform.service.AwsS3Service;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final AwsS3Service awsS3Service;
    private final UserRepository userRepository;

    public ProfileImageUploadUrlResponse createUploadUrl(ProfileImageUploadUrlRequest req) {

        String key = "profiles/" + UUID.randomUUID() + "_" + req.getFileName();

        URL uploadUrl = awsS3Service.generatePresignedUploadUrl(
                key,
                req.getMimeType()
        );

        return new ProfileImageUploadUrlResponse(uploadUrl, key);
    }

    @Transactional
    public void saveProfileImage(SaveProfileImageRequest req) {

        User user = userRepository.findById(req.getPid())
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        user.updateProfileImage(req.getImageKey());  // User 엔티티에 추가했던 메서드

    }
}