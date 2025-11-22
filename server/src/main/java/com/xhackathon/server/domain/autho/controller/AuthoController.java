package com.xhackathon.server.domain.autho.controller;

import com.xhackathon.server.domain.autho.dto.request.LoginRequest;
import com.xhackathon.server.domain.autho.dto.request.SignupRequest;
import com.xhackathon.server.domain.autho.dto.response.LoginResponse;
import com.xhackathon.server.domain.autho.dto.response.SignupResponse;
import com.xhackathon.server.domain.autho.service.AuthoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthoController {

    private final AuthoService authoService;

    public AuthoController(AuthoService authoService) {
        this.authoService = authoService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        SignupResponse response = authoService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authoService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

}
