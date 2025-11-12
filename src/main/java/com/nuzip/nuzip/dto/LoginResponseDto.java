package com.nuzip.nuzip.dto;

import jakarta.validation.constraints.NotBlank;

// 로그인 응답 DTO(authenticated, user 정보)
// 로그인 성공 시 프론트에 돌려줄 출력 DTO
// 비밀번호/내부키 등을 절대 노출하지 않기 위해 "필요한 정보만" 선별해서 반환
public class LoginResponseDto {
    private boolean authenticated;
    private Long id;
    private String userId;
    private String username;

    public LoginResponseDto() {}

    public LoginResponseDto(boolean authenticated, Long id, String userId, String username) {
        this.authenticated = authenticated;
        this.id = id;
        this.userId = userId;
        this.username = username;
    }

    public LoginResponseDto(boolean b, Long id, @NotBlank(message = "아이디는 필수 입력값입니다.") String userId, @NotBlank(message = "이름은 필수 입력값입니다.") String username, String token) {
    }

    public boolean isAuthenticated() { return authenticated; }
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    public void setId(Long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
}
