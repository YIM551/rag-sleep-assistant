package com.sleepwell.sleepwell_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 수면 설정 컨트롤러 테스트용 간단한 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/test/sleep-settings")
@RequiredArgsConstructor
@Tag(name = "Sleep Setting Test", description = "수면 설정 테스트 API")
public class SleepSettingTestController {

    @Operation(
            summary = "컨트롤러 상태 확인",
            description = "수면 설정 컨트롤러가 정상적으로 등록되었는지 확인하는 핑 API"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "컨트롤러 정상 작동")
    })
    @GetMapping("/ping")
    public String ping() {
        log.info("SleepSettingTestController ping 호출됨");
        return "pong - 컨트롤러 등록 성공!";
    }
}