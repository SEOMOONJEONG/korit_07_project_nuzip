package com.nuzip.nuzip.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

// 로그인 성공 시 헤더에 토큰발급
@Service
public class JwtService {

    static final long EXPIRATION_TIME = 86400000; // 하루
    static final String PREFIX = "Bearer ";

    // ✅ 고정 secret 사용 (서버 재시작 시 토큰 무효화 방지)
    private static final String SECRET = "replace-this-with-your-own-64-byte-secret-key-string...........";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // ✅ JWT 생성 (subject = userId)
    public String generateToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // ✅ 요청 헤더에서 JWT 추출 후 사용자 ID(subject) 반환
    public String getAuthUser(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(token != null && token.startsWith(PREFIX)) {
            try {
                String user = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token.replace(PREFIX, ""))   // 접두사 "Bearer " 제거
                        .getPayload()
                        .getSubject();

                if(user != null) {
                    return user;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;    // getAUthUser 메서드 호출했는데 user 안튀어나올 때 null return : 토큰이 없거나 유효하지 않음.
    }

    public boolean isValidToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private final Key signingKey;   // jwt 서명 검증 비밀키

    public JwtService(@Value("${jwt.secret}") String secret) {  // 스프링 의존성 주입 키
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());    // 문자열 키를 실제 암호화 키로 변환하는 과정
    }

    // 회원정보 수정 전 비밀번호 확인 과정에서 임시 토큰 발급 받음
    // 용도 구분하여 로그인 토큰과 회원정보 임시 토큰과 구분하도록 함
    public String issueReverifyToken(String userId, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .setAudience("reverify")    // 용도 구분 (로그인 토큰이냐, 수정용 임시토큰이냐 구분)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .claim("scope", "profile:edit") // 권한 범위 명시 (행동 범위)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean verifyReverifyToken(String token, String expectedUserId) {
        var claims = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (!"reverify".equals(claims.getAudience())) return false;
        if (!expectedUserId.equals(claims.getSubject())) return false;
        // 만료는 파서에서 자동 검증됨
        return true;
    }
}