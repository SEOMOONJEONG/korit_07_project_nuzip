package com.nuzip.nuzip.service;

import com.nuzip.nuzip.domain.AuthProvider;
import com.nuzip.nuzip.domain.User;
import com.nuzip.nuzip.domain.UserRepository;
import com.nuzip.nuzip.domain.NewsCategory;
import com.nuzip.nuzip.dto.*;
import com.nuzip.nuzip.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;

// 검증 통과한 DTO → 엔티티 생성/저장 → 응답 DTO로 변환
// 비즈니스 로직 : 중복체크/암호화/검증
// 컨트롤러에서 받은 DTO를 바탕으로 자격 검증 수행
// 컨트롤러를 얇게 유지, 로직 재사용/테스트 용이
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;   // DB 접근용 (회원 정보 조회/저장)
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화/검증
    private final JwtService jwtService;           // JWT 생성/검증용 서비스


    /**
     * 회원가입
     * - 1단계에서 카테고리는 없어도 됨(null 허용)
     */
    @Transactional
    public RegisterResponseDto register(RegisterRequestDto req) {
        if (userRepository.existsByUserId(req.getUserId())) {
            throw new IllegalArgumentException("이미 회원가입 된 아이디입니다.");
        }

        User user = new User();
        user.setUserId(req.getUserId());
        user.setPassword(passwordEncoder.encode(req.getPassword())); // 반드시 해시 저장
        user.setUsername(req.getUsername());
        // [CHANGED] 카테고리는 2단계에서 저장. 1단계에서 값이 온 경우에만 검증 후 반영
        if (req.getNewsCategory() != null) {
            if (req.getNewsCategory().size() > 3) {
                throw new IllegalArgumentException("카테고리는 최대 3개까지만 선택할 수 있습니다.");
            }
            user.setNewsCategory(req.getNewsCategory());
        }
        user.setBirthDate(req.getBirthDate());                       // 선택값
        user.setPhone(req.getPhone());                               // 숫자만(11자리) @Pattern 검증

        User saved = userRepository.save(user);

        return new RegisterResponseDto(
                saved.getId(),
                saved.getUserId(),
                saved.getUsername()
        );
    }

    /**
     * 🔐 로그인(login)
     * - 아이디(userId)로 사용자 조회
     * - 비밀번호 검증 후 성공시 JWT 발급
     * - 프론트엔드로 LoginResponseDto 반환 (토큰 포함)
     *   (=프론트가 이후 요청에서 Authorization: Bearer <token> 으로 사용)
     */
    public LoginResponseDto login(AccountCredentialsDto creds) {
        var user = userRepository.findByUserId(creds.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(creds.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // JWT 토큰 생성 (subject = userId)
        String token = jwtService.generateToken(user.getUserId());

        return new LoginResponseDto(
                true,
                user.getId(),
                user.getUserId(),
                user.getUsername(),
                token
        );
    }

    /**
     * 2단계: 관심 카테고리 정확히 3개 저장
     * - 인증된 사용자(userId 기준)
     */

    @Transactional // [ADDED]
    public void updateCategories(String userId, List<String> categories) {
        if (categories == null || categories.size() != 3) {
            throw new IllegalArgumentException("카테고리는 정확히 3개를 선택해야 합니다.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 문자열 → enum 매핑(유효하지 않은 값이면 IllegalArgumentException 발생)
        Set<NewsCategory> set = categories.stream()
                .map(NewsCategory::valueOf)
                .collect(Collectors.toSet());

        if (set.size() != 3) { // 중복 선택 방지
            throw new IllegalArgumentException("카테고리는 중복 없이 3개를 선택해야 합니다.");
        }

        user.setNewsCategory(set); // JPA dirty checking으로 업데이트
    }

    @Transactional(readOnly = true)
    public Set<NewsCategory> getCategories(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Set<NewsCategory> categories = user.getNewsCategory();
        if (categories == null) {
            return Set.of();
        }
        return categories.stream().collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public User getUserOrThrow(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }


    /**
     * 현재 비밀번호 검증
     * - 규칙: LOCAL에서만 사용. OAUTH_GOOGLE 계정은 항상 false 반환(또는 예외).
     */
    // 사용자가 입력한 비밀번호가 DB에 저장된 암호화된 비밀번호와 일치하는지 확인하는 로직
    // /me/verify-password API 검증 부분
    public boolean verifyCurrentPassword(String userId, String rawPassword) {
        var user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getProvider() != AuthProvider.LOCAL) {
            return false; // 구글 계정은 로컬 비번 검증 대상이 아님
        }
        if (!StringUtils.hasText(user.getPassword())) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public AuthProvider getProvider(String userId) {
        return userRepository.findByUserId(userId)
                .map(User::getProvider)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 회원정보 수정(프로필 + 옵션: 비밀번호 변경)
     * - 규칙
     *   1) 프로필(username/phone/birthDate)은 모두 계정 유형과 무관하게 수정 가능
     *   2) 비밀번호 변경은 LOCAL만 가능
     *      - OAUTH_GOOGLE이 newPassword를 보내면 403(IllegalStateException) 던짐
     *      - LOCAL인 경우에만 currentPassword 검증 후 변경
     */
    @Transactional
    public void updateMyInfo(String userId, UpdateMyInfoRequestDto updateMyInfoReq) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 1) 프로필 필드 부분 수정(Null 아닌 값만 반영)
        if(updateMyInfoReq.getUsername() != null) user.setUsername(updateMyInfoReq.getUsername());
        if(updateMyInfoReq.getPhone() != null) user.setPhone(updateMyInfoReq.getPhone());
        if(updateMyInfoReq.getBirthDate() != null) user.setBirthDate(updateMyInfoReq.getBirthDate());
        // 2) 카테고리 교체 (요청에 categories가 왔을 때만 반영)
        if (updateMyInfoReq.getCategories() != null) {
            if (updateMyInfoReq.getCategories().size() != 3) {
                throw new IllegalArgumentException("카테고리는 정확히 3개를 선택해야 합니다.");
            }

            Set<NewsCategory> newCats = updateMyInfoReq.getCategories().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return NewsCategory.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("유효하지 않은 카테고리: " + s);
                        }
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (newCats.size() != 3) {
                throw new IllegalArgumentException("카테고리는 중복 없이 정확히 3개를 선택해야 합니다.");
            }

            user.setNewsCategory(newCats);
        }

        // 3) 비밀번호 변경 요청이 있는지 확인
        boolean wantsPwChange =
                StringUtils.hasText(updateMyInfoReq.getNewPassword()) ||
                        StringUtils.hasText(updateMyInfoReq.getConfirmNewPassword());

        if (!wantsPwChange) {
            return;
        }

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("구글 계정은 로컬 비밀번호 변경이 허용되지 않습니다.");
        }

        String newPassword = updateMyInfoReq.getNewPassword();
        String confirmPassword = updateMyInfoReq.getConfirmNewPassword();

        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호를 모두 입력해야 합니다.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }
}