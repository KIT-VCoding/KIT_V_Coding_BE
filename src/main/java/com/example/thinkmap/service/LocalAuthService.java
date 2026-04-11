package com.example.thinkmap.service;

import com.example.thinkmap.domain.entity.AuthProvider;
import com.example.thinkmap.domain.entity.User;
import com.example.thinkmap.domain.repository.UserRepository;
import com.example.thinkmap.dto.auth.AuthResponse;
import com.example.thinkmap.dto.auth.LoginRequest;
import com.example.thinkmap.dto.auth.SignUpRequest;
import com.example.thinkmap.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .provider(AuthProvider.LOCAL)
                .providerId(req.email())   // LOCAL 유저는 email을 providerId로 사용
                .build();

        User saved = userRepository.save(user);
        return new AuthResponse(jwtTokenProvider.createTokenByUserId(saved.getId()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .filter(u -> u.getProvider() == AuthProvider.LOCAL)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return new AuthResponse(jwtTokenProvider.createTokenByUserId(user.getId()));
    }
}
