package com.nuzip.nuzip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

@Getter
public class UpdateMyInfoRequestDto {

    // 허용한 수정 항목
    private String username;    // 닉네임
    private Set<String> categories; // 카테고리 수정
    private LocalDate birthDate;    // 생년월일
    private String phone;       // 전화번호

    // ✅ 비밀번호 변경용 (LOCAL만 사용)
    private String newPassword; // 비밀번호 변경 요청 시 전달
    private String confirmNewPassword;   // 새 비밀번호 확인

}
