package com.sleepwell.sleepwell_backend.validation;

import com.sleepwell.sleepwell_backend.dto.PasswordChangeRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 비밀번호 일치 검증 로직 구현체
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, PasswordChangeRequestDto> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        // 초기화 로직 (필요시)
    }

    @Override
    public boolean isValid(PasswordChangeRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true; // null은 다른 validator에서 처리
        }
        
        if (dto.getNewPassword() == null || dto.getConfirmPassword() == null) {
            return true; // null은 @NotBlank에서 처리
        }
        
        return dto.getNewPassword().equals(dto.getConfirmPassword());
    }
} 