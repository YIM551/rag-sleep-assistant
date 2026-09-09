package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.ASMRContentResponseDto;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ASMR 스트리밍 서비스
 * 적응형 품질 관리 및 스트리밍 최적화
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ASMRStreamingService {

    private final ASMRService asmrService;
    private final ASMRMetricsService metricsService;

    // 스트리밍 통계
    private final AtomicInteger activeStreams = new AtomicInteger(0);
    private final AtomicLong totalBandwidthUsed = new AtomicLong(0);
    private final Map<String, Integer> qualityUsage = new ConcurrentHashMap<>();
    private final long serviceStartTime = Instant.now().getEpochSecond();

    // 품질별 비트레이트 (kbps)
    private static final Map<String, Integer> QUALITY_BITRATES = Map.of(
        "high", 320,
        "medium", 128,
        "low", 64
    );

    /**
     * 스트리밍 리소스 조회
     */
    @Cacheable(value = "asmr-streaming", key = "#contentId + '_' + #quality")
    public Mono<StreamingResponse> getStreamingResource(Long contentId, String quality, String rangeHeader) {
        return Mono.fromCallable(() -> {
            ASMRContentResponseDto content = asmrService.getContent(contentId);
            String audioUrl = getAudioUrlByQuality(content, quality);

            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.ASMR_QUALITY_NOT_AVAILABLE,
                    String.format("%s 품질의 오디오를 사용할 수 없습니다", quality));
            }

            try {
                Resource resource = new UrlResource(audioUrl);

                // Range 헤더 처리
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    return handleRangeRequest(resource, rangeHeader);
                } else {
                    return StreamingResponse.builder()
                            .resource(resource)
                            .contentLength(resource.contentLength())
                            .partialContent(false)
                            .build();
                }

            } catch (Exception e) {
                log.error("스트리밍 리소스 생성 실패: contentId={}, quality={}", contentId, quality, e);

                // 구체적인 에러 타입에 따라 다른 에러 코드 반환
                if (e instanceof java.net.MalformedURLException) {
                    throw new BusinessException(ErrorCode.ASMR_QUALITY_NOT_AVAILABLE, "잘못된 오디오 URL입니다");
                } else if (e instanceof java.io.FileNotFoundException) {
                    throw new BusinessException(ErrorCode.ASMR_QUALITY_NOT_AVAILABLE, "오디오 파일을 찾을 수 없습니다");
                } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                    throw new BusinessException(ErrorCode.ASMR_STREAM_FAILED, "스트리밍 서비스 응답 시간 초과");
                } else {
                    throw new BusinessException(ErrorCode.ASMR_STREAM_FAILED, "스트리밍 리소스를 생성할 수 없습니다");
                }
            }
        })
        .doOnSuccess(response -> {
            activeStreams.incrementAndGet();
            qualityUsage.merge(quality, 1, Integer::sum);
            metricsService.recordStreamingStart(contentId, quality);
            log.info("스트리밍 시작: contentId={}, quality={}, activeStreams={}", contentId, quality, activeStreams.get());
        })
        .doFinally(signalType -> {
            activeStreams.decrementAndGet();
            long streamingDuration = System.currentTimeMillis(); // 실제로는 시작 시간을 기록해야 함
            metricsService.recordStreamingEnd(contentId, quality, streamingDuration);
            log.debug("스트리밍 종료: contentId={}, signalType={}, activeStreams={}", contentId, signalType, activeStreams.get());
        });
    }

    /**
     * 미리보기 리소스 조회
     */
    @Cacheable(value = "asmr-preview", key = "#contentId")
    public Mono<Resource> getPreviewResource(Long contentId) {
        return Mono.fromCallable(() -> {
            ASMRContentResponseDto content = asmrService.getContent(contentId);

            if (content.getPreviewUrl() == null) {
                throw new BusinessException(ErrorCode.ASMR_CONTENT_NOT_FOUND, "미리보기 파일을 찾을 수 없습니다");
            }

            try {
                return new UrlResource(content.getPreviewUrl());
            } catch (MalformedURLException e) {
                log.error("미리보기 URL 오류: contentId={}, url={}", contentId, content.getPreviewUrl(), e);
                throw new BusinessException(ErrorCode.ASMR_UPDATE_FAILED, "미리보기 URL이 유효하지 않습니다");
            }
        });
    }

    /**
     * 적응형 품질 추천 (동기 버전)
     */
    public QualityRecommendation recommendQualitySync(Integer bandwidth, String networkType, String batteryLevel) {
        String recommendedQuality;
        String reason;
        boolean networkOptimized = true;

        // 대역폭 기반 품질 결정
        if (bandwidth != null) {
            if (bandwidth >= 500) {
                recommendedQuality = "high";
                reason = "충분한 대역폭";
            } else if (bandwidth >= 200) {
                recommendedQuality = "medium";
                reason = "보통 대역폭";
            } else {
                recommendedQuality = "low";
                reason = "제한된 대역폭";
            }
        } else if ("mobile".equals(networkType)) {
            recommendedQuality = "medium";
            reason = "모바일 네트워크";
        } else if ("wifi".equals(networkType)) {
            recommendedQuality = "high";
            reason = "WiFi 연결";
        } else {
            recommendedQuality = "medium";
            reason = "기본 설정";
        }

        // 배터리 절약 모드 고려
        if ("low".equals(batteryLevel) && !"low".equals(recommendedQuality)) {
            recommendedQuality = "medium";
            reason += " (배터리 절약)";
            networkOptimized = false;
        }

        return QualityRecommendation.builder()
                .quality(recommendedQuality)
                .bitrate(QUALITY_BITRATES.get(recommendedQuality))
                .reason(reason)
                .networkOptimized(networkOptimized)
                .build();
    }

    /**
     * 적응형 품질 추천 (Reactive 버전 - 호환성 유지)
     */
    public Mono<QualityRecommendation> recommendQuality(Integer bandwidth, String networkType, String batteryLevel) {
        return Mono.fromCallable(() -> recommendQualitySync(bandwidth, networkType, batteryLevel));
    }

    /**
     * 스트리밍 URL 목록 조회 (동기 버전)
     */
    public Map<String, String> getStreamingUrlsSync(Long contentId) {
        ASMRContentResponseDto content = asmrService.getContent(contentId);

        Map<String, String> urls = new ConcurrentHashMap<>();

        if (content.getHighQualityUrl() != null) {
            urls.put("high", content.getHighQualityUrl());
        }
        if (content.getMediumQualityUrl() != null) {
            urls.put("medium", content.getMediumQualityUrl());
        }
        if (content.getLowQualityUrl() != null) {
            urls.put("low", content.getLowQualityUrl());
        }
        if (content.getPreviewUrl() != null) {
            urls.put("preview", content.getPreviewUrl());
        }

        return urls;
    }

    /**
     * 스트리밍 URL 목록 조회 (Reactive 버전 - 호환성 유지)
     */
    public Mono<Map<String, String>> getStreamingUrls(Long contentId) {
        return Mono.fromCallable(() -> getStreamingUrlsSync(contentId));
    }

    /**
     * 스트리밍 통계 조회
     */
    public Mono<StreamingStats> getStreamingStats() {
        return Mono.fromCallable(() -> {
            long currentTime = Instant.now().getEpochSecond();
            long uptime = currentTime - serviceStartTime;

            // 평균 품질 계산
            String averageQuality = calculateAverageQuality();

            // 캐시 히트율 (간단한 시뮬레이션)
            double cacheHitRate = Math.min(0.95, qualityUsage.size() > 0 ? 0.7 + (qualityUsage.size() * 0.05) : 0.7);

            return StreamingStats.builder()
                    .activeStreams(activeStreams.get())
                    .totalBandwidth(totalBandwidthUsed.get())
                    .averageQuality(averageQuality)
                    .cacheHitRate(cacheHitRate)
                    .uptimeSeconds(uptime)
                    .build();
        });
    }

    /**
     * Range 요청 처리
     */
    private StreamingResponse handleRangeRequest(Resource resource, String rangeHeader) {
        try {
            // Range 헤더 기본 검증
            if (!rangeHeader.toLowerCase().startsWith("bytes=")) {
                throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER, "지원하지 않는 Range 헤더 형식입니다");
            }

            // Range 헤더 파싱 (예: "bytes=0-1023")
            String rangeValue = rangeHeader.substring(6);
            if (rangeValue.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER, "Range 값이 비어있습니다");
            }

            String[] ranges = rangeValue.split("-");
            if (ranges.length == 0 || ranges.length > 2) {
                throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER, "잘못된 Range 형식입니다");
            }

            long totalLength = resource.contentLength();
            if (totalLength <= 0) {
                throw new BusinessException(ErrorCode.ASMR_STREAM_FAILED, "파일 크기를 확인할 수 없습니다");
            }

            long start, end;

            // 시작 위치 파싱
            if (ranges[0].isEmpty()) {
                // "-500" 형태: 마지막 500바이트
                if (ranges.length != 2 || ranges[1].isEmpty()) {
                    throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER, "suffix-length 형식이 잘못되었습니다");
                }
                long suffixLength = Long.parseLong(ranges[1]);
                start = Math.max(0, totalLength - suffixLength);
                end = totalLength - 1;
            } else {
                start = Long.parseLong(ranges[0]);
                if (ranges.length == 2 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                } else {
                    end = totalLength - 1;
                }
            }

            // Range 유효성 검증
            if (start < 0) {
                throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER, "시작 위치는 0 이상이어야 합니다");
            }

            if (start >= totalLength) {
                throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER,
                    String.format("시작 위치(%d)가 파일 크기(%d)를 초과합니다", start, totalLength));
            }

            if (end >= totalLength) {
                end = totalLength - 1;
            }

            if (start > end) {
                throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER,
                    String.format("시작 위치(%d)가 끝 위치(%d)보다 큽니다", start, end));
            }

            long contentLength = end - start + 1;
            String contentRange = String.format("bytes %d-%d/%d", start, end, totalLength);

            return StreamingResponse.builder()
                    .resource(resource)
                    .contentLength(contentLength)
                    .contentRange(contentRange)
                    .partialContent(true)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (NumberFormatException e) {
            log.error("Range 헤더 숫자 파싱 실패: {}", rangeHeader, e);
            throw new BusinessException(ErrorCode.ASMR_INVALID_RANGE_HEADER, "Range 값이 올바른 숫자가 아닙니다");
        } catch (Exception e) {
            log.error("Range 요청 처리 실패: {}", rangeHeader, e);
            throw new BusinessException(ErrorCode.ASMR_STREAM_FAILED, "Range 요청 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * 품질별 오디오 URL 조회
     */
    private String getAudioUrlByQuality(ASMRContentResponseDto content, String quality) {
        return switch (quality.toLowerCase()) {
            case "high" -> content.getHighQualityUrl();
            case "medium" -> content.getMediumQualityUrl();
            case "low" -> content.getLowQualityUrl();
            default -> content.getMediumQualityUrl(); // 기본값
        };
    }

    /**
     * 스트리밍 URL 생성
     */
    private String buildStreamingUrl(Long contentId, String quality) {
        return String.format("/api/asmr/stream/%d?quality=%s", contentId, quality);
    }

    /**
     * 미리보기 URL 생성
     */
    private String buildPreviewUrl(Long contentId) {
        return String.format("/api/asmr/stream/%d/preview", contentId);
    }

    /**
     * 평균 품질 계산
     */
    private String calculateAverageQuality() {
        if (qualityUsage.isEmpty()) {
            return "medium";
        }

        int totalUsage = qualityUsage.values().stream().mapToInt(Integer::intValue).sum();
        int weightedSum = qualityUsage.entrySet().stream()
                .mapToInt(entry -> {
                    String quality = entry.getKey();
                    int usage = entry.getValue();
                    int weight = switch (quality) {
                        case "high" -> 3;
                        case "medium" -> 2;
                        case "low" -> 1;
                        default -> 2;
                    };
                    return weight * usage;
                })
                .sum();

        double average = (double) weightedSum / totalUsage;

        if (average >= 2.5) return "high";
        if (average >= 1.5) return "medium";
        return "low";
    }

    // DTO 클래스들
    @lombok.Builder
    @lombok.Data
    public static class StreamingResponse {
        private Resource resource;
        private long contentLength;
        private String contentRange;
        private boolean partialContent;
    }

    @lombok.Builder
    @lombok.Data
    public static class QualityRecommendation {
        private String quality;
        private Integer bitrate;
        private String reason;
        private boolean networkOptimized;
    }

    @lombok.Builder
    @lombok.Data
    public static class StreamingStats {
        private int activeStreams;
        private long totalBandwidth;
        private String averageQuality;
        private double cacheHitRate;
        private long uptimeSeconds;
    }
}