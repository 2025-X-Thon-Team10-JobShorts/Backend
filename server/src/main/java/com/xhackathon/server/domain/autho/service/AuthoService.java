package com.xhackathon.server.domain.autho.service;

import com.xhackathon.server.domain.autho.dto.request.LoginRequest;
import com.xhackathon.server.domain.autho.dto.request.SignupRequest;
import com.xhackathon.server.domain.autho.dto.response.LoginResponse;
import com.xhackathon.server.domain.autho.dto.response.SignupResponse;
import com.xhackathon.server.domain.user.entity.User;
import com.xhackathon.server.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthoService {

    private final UserRepository userRepository;

    public AuthoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String pid = buildPid(
                request.getLoginId(),
                request.getPassword(),
                request.getRole().name(),
                request.getDisplayName()
        );

        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 loginId 입니다.");
        }

        if (userRepository.existsById(pid)) {
            throw new IllegalArgumentException("이미 존재하는 pid 입니다.");
        }

        User user = new User(
                pid,
                request.getLoginId(),
                request.getPassword(),
                request.getRole(),
                request.getDisplayName()
        );

        userRepository.save(user);

        return new SignupResponse(pid);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String pid = buildPid(
                request.getLoginId(),
                request.getPassword(),
                request.getRole().name(),
                request.getDisplayName()
        );

        boolean exists = userRepository.existsById(pid);

        if (exists) {
            return new LoginResponse(true, pid, "로그인 성공");
        } else {
            return new LoginResponse(false, null, "로그인 실패: 일치하는 사용자 없음");
        }
    }

    private String buildPid(String loginId, String password, String role, String displayName) {
        return loginId + "_" + password + "_" + role + "_" + displayName;
    }
}