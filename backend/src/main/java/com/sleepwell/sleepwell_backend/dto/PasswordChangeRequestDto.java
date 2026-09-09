package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.validation.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비밀번호 변경 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@PasswordMatches
public class PasswordChangeRequestDto {

    @Schema(description = "현재 사용자의 비밀번호", example = "current1234")
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    private String currentPassword;

    @Schema(description = "새로운 비밀번호. 8~20자 길이, 영문 대/소문자, 숫자, 특수문자(@$!%*?&)를 각각 하나 이상 포함해야 합니다.", example = "NewPass123!")
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "비밀번호는 대소문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다"
    )
    private String newPassword;

    @Schema(description = "새로운 비밀번호 확인", example = "NewPass123!")
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;
} 