package com.sleepwell.sleepwell_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    
    public AuthResponseDto(String token, String email, String name, String role) {
        this.token = token;
        this.email = email;
        this.name = name;
        this.role = role;
    }
    @Schema(description = "JWT 토큰")
    private String token;

    @Schema(description = "사용자 이메일")
    private String email;

    @Schema(description = "사용자 이름")
    private String name;

    @Schema(description = "사용자 역할")
    private String role;
    
    @Schema(description = "JWT Refresh 토큰", required = false)
    private String refreshToken;
}
