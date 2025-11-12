package com.nuzip.nuzip.dto;

import com.nuzip.nuzip.domain.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EditFormMetaResponseDto {
    private AuthProvider provider;
    private boolean canChangePassword;  // LOCAL=true, OAUTH_GOOGLE=false
}

