package com.nuzip.nuzip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// 비밀번호 확인 요청
@Getter
public class VerifyPasswordRequestDto {
    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;
}
