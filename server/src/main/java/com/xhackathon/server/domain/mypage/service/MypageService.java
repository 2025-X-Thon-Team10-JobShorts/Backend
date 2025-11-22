package com.xhackathon.server.domain.mypage.service;

import com.xhackathon.server.domain.mypage.dto.request.MypageUpdateRequest;
import com.xhackathon.server.domain.mypage.dto.response.MypageResponse;
import com.xhackathon.server.domain.shortform.repository.ShortFormRepository;
import com.xhackathon.server.domain.shortform.entity.ShortForm;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final ShortFormRepository shortFormRepository;

    @Transactional(readOnly = true)
    public MypageResponse getMyPage(String pid) {
        User user = userRepository.findById(pid)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        List<String> videoKeys = shortFormRepository.findByOwnerPid(pid)
                .stream()
                .map(ShortForm::getVideoKey)
                .toList();

        return MypageResponse.from(user, videoKeys);
    }

    @Transactional
    public MypageResponse updateMyPage(MypageUpdateRequest req) {
        User user = userRepository.findById(req.getPid())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        user.updateProfile(
                req.getBio(),
                req.getPortfolioLink(),
                req.getPortfolioFileUrl()
        );

        List<String> videoKeys = shortFormRepository.findByOwnerPid(req.getPid())
                .stream()
                .map(ShortForm::getVideoKey)
                .toList();

        return MypageResponse.from(user, videoKeys);
    }
}
