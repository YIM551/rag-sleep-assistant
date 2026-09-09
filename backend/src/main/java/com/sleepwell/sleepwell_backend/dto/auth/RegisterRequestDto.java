package com.sleepwell.sleepwell_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {
    @Schema(description = "사용자 이메일", example = "newuser@sleepwell.com")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "새로운 비밀번호. 8~20자 길이, 영문, 숫자, 특수문자(@$!%*?&)를 각각 하나 이상 포함해야 합니다.", example = "newpass123!")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다"
    )
    private String password;

    @Schema(description = "사용자 이름", example = "새 사용자")
    @NotBlank(message = "이름은 필수입니다")
    private String name;
}
