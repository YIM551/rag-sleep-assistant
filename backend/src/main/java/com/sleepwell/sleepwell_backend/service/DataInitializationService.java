package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.enums.Gender;
import com.sleepwell.sleepwell_backend.enums.UserRole;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 애플리케이션 시작 시 테스트용 더미 데이터를 생성하는 서비스
 * 
 * 2026: 합성 데이터 생성은 local-fixture profile에서만 명시적으로 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("local-fixture")  // 2026: explicit local synthetic-fixture opt-in only
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== 테스트 데이터 초기화 시작 ===");
        
        // 기존 데이터가 있으면 스킵
        if (userRepository.count() > 0) {
            log.info("기존 사용자 데이터가 존재합니다. 초기화를 스킵합니다.");
            return;
        }
        
        createTestUsers();
        createTestSleepRecords();
        
        log.info("=== 테스트 데이터 초기화 완료 ===");
    }

    /**
     * 테스트용 사용자들을 생성합니다.
     */
    private void createTestUsers() {
        log.info("테스트 사용자 생성 중...");
        
        // 1. 일반 테스트 사용자
        User testUser = User.builder()
                .email("test@sleepwell.com")
                .password(passwordEncoder.encode("test123"))
                .name("테스트 사용자")
                .age(33) // 1990년생 기준
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .isActive(true)
                .build();
        userRepository.save(testUser);
        log.info("테스트 사용자 생성: {}", testUser.getEmail());

        // 2. 관리자 사용자
        User adminUser = User.builder()
                .email("admin@sleepwell.com")
                .password(passwordEncoder.encode("admin123"))
                .name("관리자")
                .age(38) // 1985년생 기준
                .gender(Gender.FEMALE)
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
        userRepository.save(adminUser);
        log.info("관리자 사용자 생성: {}", adminUser.getEmail());

        // 3. 추가 테스트 사용자
        User testUser2 = User.builder()
                .email("user2@sleepwell.com")
                .password(passwordEncoder.encode("user123"))
                .name("사용자2")
                .age(28) // 1995년생 기준
                .gender(Gender.FEMALE)
                .role(UserRole.USER)
                .isActive(true)
                .build();
        userRepository.save(testUser2);
        log.info("추가 테스트 사용자 생성: {}", testUser2.getEmail());

        log.info("총 {}명의 사용자가 생성되었습니다.", userRepository.count());
    }

    /**
     * 테스트용 수면 기록들을 생성합니다.
     */
    private void createTestSleepRecords() {
        log.info("테스트 수면 기록 생성 중...");
        
        User testUser = userRepository.findByEmail("test@sleepwell.com")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다"));

        // 최근 7일간의 수면 기록 생성
        for (int i = 0; i < 7; i++) {
            LocalDate recordDate = LocalDate.now().minusDays(i);
            
            // 간단하고 안전한 시간 계산
            LocalTime[] sleepTimes = {
                LocalTime.of(23, 30), LocalTime.of(23, 45), LocalTime.of(0, 0), 
                LocalTime.of(0, 15), LocalTime.of(23, 20), LocalTime.of(23, 50), LocalTime.of(0, 10)
            };
            
            LocalTime[] wakeTimes = {
                LocalTime.of(7, 0), LocalTime.of(7, 30), LocalTime.of(8, 0), 
                LocalTime.of(7, 15), LocalTime.of(8, 30), LocalTime.of(7, 45), LocalTime.of(8, 15)
            };
            
            LocalTime sleepTime = sleepTimes[i];
            LocalTime wakeTime = wakeTimes[i];
            
            // 자정을 넘는 경우 처리
            LocalDateTime sleepStart = sleepTime.getHour() >= 22 ? 
                LocalDateTime.of(recordDate, sleepTime) : 
                LocalDateTime.of(recordDate.plusDays(1), sleepTime);
                
            LocalDateTime sleepEnd = LocalDateTime.of(recordDate.plusDays(1), wakeTime);
            
            SleepRecord sleepRecord = SleepRecord.builder()
                    .user(testUser)
                    .recordDate(recordDate)
                    .sleepStartTime(sleepStart)
                    .sleepEndTime(sleepEnd)
                    .totalSleepMinutes(420 + (i % 3) * 30) // 7시간 ~ 8시간
                    .deepSleepMinutes(90 + (i % 2) * 20) // 1.5시간 ~ 1.8시간
                    .lightSleepMinutes(240 + (i % 3) * 20) // 4시간 ~ 5시간
                    .remSleepMinutes(90 + (i % 2) * 10) // 1.5시간 ~ 1.7시간
                    .wakeupCount(i % 3) // 0~2회
                    .sleepInBedMinutes(450 + (i % 3) * 30) // 침대에 있던 시간
                    .sleepQualityScore(75 + (i % 4) * 5) // 75~90점
                    .temperature(22.0 + (i % 3) * 2) // 22~26도
                    .humidity(50.0 + (i % 2) * 10) // 50~60%
                    .lightLevel(0.5 + (i % 3) * 0.2) // 0.5~0.9 lux
                    .noiseLevel(30.0 + (i % 2) * 5) // 30~35 dB
                    .wearableSource(WearableSource.APPLE_HEALTH)
                    .heartRateData("{\"average\": " + (60 + (i % 3) * 5) + ", \"min\": " + (50 + (i % 2) * 5) + ", \"max\": " + (80 + (i % 3) * 10) + "}")
                    .build();
            
            sleepRecordRepository.save(sleepRecord);
        }
        
        log.info("총 {}개의 수면 기록이 생성되었습니다.", sleepRecordRepository.count());
    }
} 