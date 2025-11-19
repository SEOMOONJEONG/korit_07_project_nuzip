package com.nuzip.nuzip.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.nuzip.nuzip.domain.AuthProvider;
import com.nuzip.nuzip.domain.User;
import com.nuzip.nuzip.domain.UserRepository;
import com.nuzip.nuzip.security.GoogleTokenVerifier;
import com.nuzip.nuzip.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

// 구글 토큰이 진짜인지 확인하고, 처음 로그인한 사용자는 자동으로 DB에 가입시킨 뒤 JWT 발급.
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String authenticateByIdToken(String idToken) throws Exception {
        Payload payload = googleTokenVerifier.verify(idToken);
        if (payload == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        String email = (String) payload.get("email");
        Boolean emailVerified = (Boolean) payload.get("email_verified");
        String name = (String) payload.get("name");

        if (email == null || Boolean.FALSE.equals(emailVerified)) {
            throw new IllegalArgumentException("Email not verified");
        }


//        // 우리 프로젝트는 userId=email 사용 가정
//        User user = userRepository.findByUserId(email).orElseGet(() -> {
//            User u = new User();
//            u.setUserId(email);
//            u.setUsername(name != null ? name : email);
//            u.setPassword(passwordEncoder.encode("GOOGLE-" + UUID.randomUUID())); // 소셜전용 더미 비번
//            return userRepository.save(u);
//        });
//
//        // JWT subject=userId
//        return jwtService.generateToken(user.getUserId());
//    }

        // 최초 로그인 시 자동 가입
        joinIfAbsent(email, name);

        // JWT subject=userId(email)
        return jwtService.generateToken(email);
    }
        // 리다이렉트 플로우에서 쓰기위한 가입 보장 메서드
        @Transactional
        public User joinIfAbsent(String email, String name) {
            return userRepository.findByUserId(email).orElseGet(() -> {
                User u = new User();
                u.setUserId(email);
                u.setUsername(name != null ? name : email);
                u.setPassword(passwordEncoder.encode("GOOGLE-" + UUID.randomUUID())); // 소셜전용 더미 비번
                u.setProvider(AuthProvider.OAUTH_GOOGLE);   // 구글 회원인데도 LOCAL로 뜨던 문제 해결
            u.setNewsCategory(new HashSet<>());
                return userRepository.save(u);
            });

        }
}
