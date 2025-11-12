package com.nuzip.nuzip.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import jakarta.servlet.http.HttpServletResponse;


// CORS/CSRF/권한 기본설정, PasswordEncoder
// 개발초기 : 막힘 방지 + 비밀번호 암호화 제공
// 기본 Security가 켜져있어 401/403 막힘을 방지하고, PasswordEncoder를 서비스에 공급

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService; // userId 기반으로 사용자 로딩
    private final JwtService jwtService;                     // JJWT 0.13.x 기반
    private final AuthEntryPoint authEntryPoint;             // 401 응답 통일

    // ✅ 추가: OAuth2 성공 핸들러 & 커스텀 OAuth2UserService 주입
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * 비밀번호 해싱용 BCrypt 인코더
     * - 회원가입 시 평문 비밀번호를 해싱 저장
     * - 로그인 시 평문과 해시 비교
     */
    // 비밀번호 검증
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager (Spring Security 내부 인증용)
     * - OAuth2 외부 로그인이나 커스텀 로그인 로직에 필요할 수 있음
     */
    // AuthenticationManager가 필요하면 주입받아 사용 (일부 케이스에서 필요)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * JWT 인증 필터
     * - 매 요청 시 Authorization 헤더에서 JWT를 추출해 검증하고
     *   SecurityContext에 인증 정보를 등록
     */
    // JWT 필터를 빈으로 등록
    @Bean
    public JwtAuthenticationFilter authenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    /**
     * 메인 보안 설정
     * - CORS, CSRF, 세션 정책, 요청별 인증 규칙, 예외 처리, 필터 등록, OAuth2 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("🔧 SecurityConfig 초기화 중...");

        http
                // 세션 없이 JWT로만 인증하므로 CSRF 비활성화 + STATELESS
                .csrf(AbstractHttpConfigurer::disable)  // CSRF 보호 비활성화(Stateless JWT 사용)
                .cors(Customizer.withDefaults())    // CORS 설정(이하의 설정 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세선 비활성화

                // 엔드포인트 접근 규칙
                .authorizeHttpRequests(auth -> auth
                        // 체크/루트
                        .requestMatchers("/", "/ready").permitAll()
                        // 로그인(헤더로 토큰 반환), 회원가입은 공개
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/google").permitAll()
                        // OAuth2 진입/콜백은 공개
                        .requestMatchers("/oauth2/**", "/login/oauth2/code/*").permitAll()
                        // Swagger UI & OpenAPI 스펙 공개
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .permitAll()
                )
                // OAuth2 로그인 성공 시 JWT 발급 후 프론트로 리다이렉트
                .oauth2Login(oauth -> oauth
//                        .loginProcessingUrl("")
                        .successHandler(customOAuth2SuccessHandler)
                )
                // 인증 실패(미인증) 시 일관된 401 JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint))
                // UsernamePasswordAuthenticationFilter 앞에 JWT 필터 삽입
                .addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        log.info("✅ OAuth2SuccessHandler 등록 완료: {}", customOAuth2SuccessHandler.getClass().getSimpleName());


//        // 개발 중 로그인 포함 모든 HTTP 메서드 요청 허용
//        http.csrf(csrf -> csrf.disable())
//                .cors(withDefaults())
//                .authorizeHttpRequests(authorizeHttpRequests ->
//                        authorizeHttpRequests.anyRequest().permitAll());
        return http.build();
    }

    // CORS: 프론트 오리진 허용
    /**
     * CORS 설정
     * - 프론트엔드 개발 서버(5173/5174)와 통신 허용
     * - 프론트가 JWT 토큰을 포함한 요청을 보낼 수 있게 설정
//     */
    // CORS: 개발용 프론트 오리진 허용 + Authorization 헤더 노출
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization")); // ← 헤더로 받은 토큰을 프론트가 읽을 수 있도록
        configuration.setAllowCredentials(true); // 필요 시 쿠키 사용 허용(쿠키 안 쓰면 true/false 상관없음)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}