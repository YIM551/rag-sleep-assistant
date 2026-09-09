package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.ASMRCategory;
import com.sleepwell.sleepwell_backend.enums.ASMRContentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ASMR 콘텐츠 엔티티 (ASMRContent Entity)
 *
 * 수면 개선을 위한 ASMR 오디오 콘텐츠의 메타데이터를 관리하는 엔티티입니다.
 * 실제 오디오 파일은 CDN에 저장되고, 여기서는 메타데이터와 스트리밍 정보만 관리합니다.
 *
 * 주요 데이터:
 * - 콘텐츠 정보 (제목, 설명, 카테고리)
 * - 오디오 메타데이터 (재생시간, 파일크기, 품질)
 * - 스트리밍 URL (품질별 URL, 미리보기 URL)
 * - 사용 통계 (재생횟수, 완주율, 평점)
 *
 * @author SleepWell Development Team
 * @since 1.0
 * @see ASMRCategory
 * @see ASMRContentStatus
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(indexes = {
    @Index(name = "IDX_ASMR_CATEGORY_STATUS", columnList = "category, status"),
    @Index(name = "IDX_ASMR_POPULARITY", columnList = "totalPlayCount, rating"),
    @Index(name = "IDX_ASMR_DURATION", columnList = "durationMinutes, category")
})
public class ASMRContent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ASMRCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ASMRContentStatus status = ASMRContentStatus.DRAFT;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false)
    private Integer audioQuality; // kbps

    @Column(precision = 8, scale = 2)
    private BigDecimal fileSizeMB;

    @Column(length = 500)
    private String highQualityUrl; // 320kbps

    @Column(length = 500)
    private String mediumQualityUrl; // 128kbps

    @Column(length = 500)
    private String lowQualityUrl; // 64kbps

    @Column(length = 500)
    private String previewUrl; // 30초 샘플

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(length = 50)
    private String targetMood; // SLEEP, RELAXATION, FOCUS

    @Column(nullable = false)
    @Builder.Default
    private Integer intensityLevel = 5; // 1-10

    @Builder.Default
    @Column(nullable = false)
    private Long totalPlayCount = 0L;

    @Builder.Default
    private Long completePlayCount = 0L;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(4.0);

    @Builder.Default
    private Integer ratingCount = 0;

    @Column(length = 100)
    private String creator;

    @Builder.Default
    private Boolean isPremium = false;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    private LocalDateTime expiresAt;

    /**
     * 인기 콘텐츠 여부 판단
     */
    public boolean isPopular() {
        return totalPlayCount >= 1000L && rating.compareTo(BigDecimal.valueOf(4.0)) >= 0;
    }

    /**
     * 품질별 URL 반환
     */
    public String getUrlByQuality(String quality) {
        return switch (quality.toLowerCase()) {
            case "high", "320" -> highQualityUrl;
            case "medium", "128" -> mediumQualityUrl;
            case "low", "64" -> lowQualityUrl;
            default -> mediumQualityUrl;
        };
    }

    /**
     * 재생 통계 업데이트
     */
    public void updatePlayStats(boolean isCompleted) {
        this.totalPlayCount++;
        if (isCompleted) {
            this.completePlayCount++;
        }
        this.lastUpdated = LocalDateTime.now();
    }
}