package com.nuzip.nuzip.dto;

import com.nuzip.nuzip.domain.NewsCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

// 회원가입 요청 DTO(검증 포함)
// 클라이언트 입력을 안전하게 받기(+ 유효성 검사)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank
    @Size(max = 50)
    private String userId;

    @NotBlank
    private String password;

    @NotBlank
    @Size(max = 50)
    private String username;

    @Size(max = 3, message = "카테고리는 최대 3개를 선택해야 합니다.")
    private Set<NewsCategory> newsCategory;

    private LocalDate birthDate;

    @Pattern(regexp = "^$ | ^\\d{11}$", message = "핸드폰 번호는 숫자만 입력(예: 01011112222)")
    private String phone;
}