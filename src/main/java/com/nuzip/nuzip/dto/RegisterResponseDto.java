package com.nuzip.nuzip.dto;

import lombok.Getter;
import lombok.Setter;

// 회원가입 응답 DTO
// 저장 결과에서 필요한 정보만 내려주기
@Setter
@Getter
public class RegisterResponseDto {
    private Long id;
    private String userId;
    private String username;

    public RegisterResponseDto() {}

    public RegisterResponseDto(Long id, String userId, String username) {
        this.id = id;
        this.userId = userId;
        this.username = username;
    }

}
