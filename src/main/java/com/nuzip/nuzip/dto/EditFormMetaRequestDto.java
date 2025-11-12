package com.nuzip.nuzip.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

// 회원정보 수정 요청(Request)
@Getter
public class EditFormMetaRequestDto {
    private String username;
    private Set<String> categories;
    private LocalDate birthDate;
    private String phone;
    private String newPassword; // LOCAL에서만 처리
}
