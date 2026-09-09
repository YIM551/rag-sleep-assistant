package com.sleepwell.sleepwell_backend.config;

import com.sleepwell.sleepwell_backend.entity.ASMRContent;
import com.sleepwell.sleepwell_backend.enums.ASMRCategory;
import com.sleepwell.sleepwell_backend.enums.ASMRContentStatus;
import com.sleepwell.sleepwell_backend.repository.ASMRContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ASMR 콘텐츠 초기 데이터 생성
 *
 * S3 연결 없이도 API 테스트가 가능하도록 목업 데이터를 생성합니다.
 * 실제 파일은 없지만 메타데이터로 시스템 동작을 확인할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "prod"}) // test 프로필에서는 실행하지 않음
public class ASMRDataInitializer implements CommandLineRunner {

    private final ASMRContentRepository asmrContentRepository;

    @Override
    public void run(String... args) {
        if (asmrContentRepository.count() == 0) {
            log.info("🎵 ASMR 초기 데이터 생성 시작...");
            createMockASMRContents();
            log.info("✅ ASMR 초기 데이터 생성 완료");
        } else {
            log.info("📚 ASMR 콘텐츠가 이미 존재합니다. (총 {}개)", asmrContentRepository.count());
        }
    }

    private void createMockASMRContents() {
        // 1. 자연음 카테고리
        createASMRContent(
            "빗소리와 천둥소리",
            "깊은 잠을 위한 자연스러운 빗소리와 은은한 천둥소리",
            ASMRCategory.NATURE,
            30,
            "SLEEP",
            3,
            "rain,thunder,nature,sleep"
        );

        createASMRContent(
            "바다 파도소리",
            "해변의 잔잔한 파도소리로 마음을 평온하게",
            ASMRCategory.NATURE,
            45,
            "RELAXATION",
            2,
            "ocean,waves,beach,calm"
        );

        createASMRContent(
            "숲속 새소리",
            "이른 아침 숲속의 다양한 새들의 지저귐",
            ASMRCategory.NATURE,
            25,
            "FOCUS",
            4,
            "forest,birds,morning,nature"
        );

        // 2. 화이트노이즈 카테고리
        createASMRContent(
            "브라운 노이즈",
            "깊은 수면을 위한 저주파 브라운 노이즈",
            ASMRCategory.WHITE_NOISE,
            60,
            "SLEEP",
            1,
            "brown,noise,deep,sleep"
        );

        createASMRContent(
            "핑크 노이즈",
            "집중력 향상을 위한 균형잡힌 핑크 노이즈",
            ASMRCategory.WHITE_NOISE,
            40,
            "FOCUS",
            2,
            "pink,noise,focus,concentration"
        );

        // 3. 가이드 음성 카테고리
        createASMRContent(
            "호흡 명상 가이드",
            "초보자를 위한 간단한 호흡 명상 가이드",
            ASMRCategory.GUIDED,
            20,
            "RELAXATION",
            3,
            "meditation,breathing,guide,beginner"
        );

        createASMRContent(
            "바디 스캔 명상",
            "몸의 긴장을 풀어주는 바디 스캔 명상",
            ASMRCategory.GUIDED,
            35,
            "SLEEP",
            4,
            "body,scan,relaxation,tension"
        );

        // 4. 수면 유도 가이드 카테고리
        createASMRContent(
            "수면 이야기 - 별자리",
            "잠들기 전 듣는 평온한 별자리 이야기",
            ASMRCategory.GUIDED,
            28,
            "SLEEP",
            5,
            "story,stars,constellation,bedtime"
        );

        createASMRContent(
            "수면 이야기 - 숲속 여행",
            "상상 속 숲속 여행으로 마음을 편안하게",
            ASMRCategory.GUIDED,
            32,
            "SLEEP",
            4,
            "story,forest,journey,imagination"
        );

        // 5. 바이노럴 비트 카테고리
        createASMRContent(
            "델타파 8Hz",
            "깊은 수면을 위한 델타파 바이노럴 비트",
            ASMRCategory.BINAURAL,
            50,
            "SLEEP",
            6,
            "delta,8hz,deep,sleep,binaural"
        );
    }

    private void createASMRContent(String title, String description, ASMRCategory category,
                                 int durationMinutes, String targetMood, int intensityLevel, String tags) {

        // 목업 URL - 실제 파일은 없지만 시스템 테스트용
        String baseUrl = "https://mock-asmr-cdn.sleepwell.com";
        String fileId = title.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        ASMRContent content = ASMRContent.builder()
                .title(title)
                .description(description)
                .category(category)
                .status(ASMRContentStatus.ACTIVE)
                .durationMinutes(durationMinutes)
                .durationSeconds(durationMinutes * 60)
                .audioQuality(128)
                .fileSizeMB(BigDecimal.valueOf(durationMinutes * 2.5)) // 대략적인 크기
                .highQualityUrl(baseUrl + "/320/" + fileId + ".mp3")
                .mediumQualityUrl(baseUrl + "/128/" + fileId + ".mp3")
                .lowQualityUrl(baseUrl + "/64/" + fileId + ".mp3")
                .previewUrl(baseUrl + "/preview/" + fileId + "_30s.mp3")
                .thumbnailUrl(baseUrl + "/thumbnails/" + fileId + ".jpg")
                .tags(tags)
                .targetMood(targetMood)
                .intensityLevel(intensityLevel)
                .totalPlayCount(0L)
                .completePlayCount(0L)
                .rating(BigDecimal.valueOf(4.0 + (Math.random() * 1.0))) // 4.0-5.0 랜덤
                .ratingCount((int)(Math.random() * 50) + 10) // 10-60 랜덤
                .creator("SleepWell Studio")
                .isPremium(Math.random() > 0.7) // 30% 확률로 프리미엄
                .isActive(true)
                .lastUpdated(LocalDateTime.now())
                .build();

        asmrContentRepository.save(content);
        log.debug("📝 ASMR 콘텐츠 생성: {} ({}분)", title, durationMinutes);
    }
}