package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.ISIResponseDto;
import com.sleepwell.sleepwell_backend.dto.ISIResponseRequestDto;
import com.sleepwell.sleepwell_backend.entity.ISIResponse;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.ISIResponseRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ISI 설문 서비스
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ISIService {

    private final ISIResponseRepository isiResponseRepository;
    private final UserRepository userRepository;

    /**
     * ISI 설문 응답 제출
     */
    @Transactional
    public ISIResponseDto submitResponse(Long userId, ISIResponseRequestDto requestDto) {
        log.info("ISI 설문 응답 제출 시작: 사용자 ID {}", userId);

        // DTO 유효성 검증
        requestDto.validateDataIntegrity();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 총점 및 해석 계산
        int totalScore = requestDto.getQ1a() + requestDto.getQ1b() + requestDto.getQ1c() +
                        requestDto.getQ2() + requestDto.getQ3() + requestDto.getQ4() + requestDto.getQ5();
        String interpretation = generateInterpretation(totalScore);

        // Entity 생성 및 저장
        ISIResponse response = ISIResponse.builder()
                .user(user)
                .q1a(requestDto.getQ1a())
                .q1b(requestDto.getQ1b())
                .q1c(requestDto.getQ1c())
                .q2(requestDto.getQ2())
                .q3(requestDto.getQ3())
                .q4(requestDto.getQ4())
                .q5(requestDto.getQ5())
                .totalScore(totalScore)
                .interpretation(interpretation)
                .build();

        response.validateResponses();
        ISIResponse savedResponse = isiResponseRepository.save(response);

        log.info("ISI 설문 응답 저장 완료: 응답 ID {}, 사용자 ID {}, 총점 {}, 해석: {}",
                savedResponse.getId(), userId, totalScore, interpretation);

        return ISIResponseDto.from(savedResponse);
    }

    /**
     * 사용자의 최신 ISI 응답 조회
     */
    public ISIResponseDto getLatestResponse(Long userId) {
        log.debug("최신 ISI 응답 조회: 사용자 ID {}", userId);

        ISIResponse response = isiResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ISI_RESPONSE_NOT_FOUND));

        return ISIResponseDto.from(response);
    }

    /**
     * 사용자의 모든 ISI 응답 이력 조회
     */
    public List<ISIResponseDto> getAllResponses(Long userId) {
        log.debug("전체 ISI 응답 이력 조회: 사용자 ID {}", userId);

        List<ISIResponse> responses = isiResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .map(ISIResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 최근 N개 ISI 응답 조회
     */
    public List<ISIResponseDto> getRecentResponses(Long userId, int limit) {
        log.debug("최근 {} 개 ISI 응답 조회: 사용자 ID {}", limit, userId);

        List<ISIResponse> responses = isiResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .limit(limit)
                .map(ISIResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 점수 기반 해석 생성
     */
    private String generateInterpretation(int totalScore) {
        if (totalScore >= 0 && totalScore <= 7) {
            return "불면증 없음";
        } else if (totalScore <= 14) {
            return "경도 불면증";
        } else if (totalScore <= 21) {
            return "중등도 불면증";
        } else if (totalScore <= 28) {
            return "중증 불면증";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "ISI 총점이 유효 범위(0-28)를 벗어났습니다: " + totalScore);
    }
}
