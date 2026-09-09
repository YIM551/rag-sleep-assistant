package com.sleepwell.sleepwell_backend.adapter;

import com.sleepwell.sleepwell_backend.enums.WearableSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 플랫폼 수면 데이터 어댑터 팩토리
 * 
 * 웨어러블 소스에 따라 적절한 어댑터를 제공하는 팩토리 클래스입니다.
 * Spring Boot 베스트 프랙티스를 적용한 의존성 주입과 팩토리 패턴을 구현합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformSleepDataAdapterFactory {

    private final List<PlatformSleepDataAdapter> adapters;

    /**
     * 웨어러블 소스에 맞는 어댑터를 반환합니다.
     * 
     * @param source 웨어러블 데이터 소스
     * @return 해당 소스를 지원하는 어댑터
     * @throws IllegalArgumentException 지원하지 않는 소스인 경우
     */
    @NonNull
    public PlatformSleepDataAdapter getAdapter(@NonNull WearableSource source) {
        log.debug("Finding adapter for wearable source: {}", source);
        
        Optional<PlatformSleepDataAdapter> adapter = adapters.stream()
                .filter(a -> a.supports(source))
                .findFirst();
        
        if (adapter.isEmpty()) {
            log.error("No adapter found for wearable source: {}", source);
            throw new IllegalArgumentException("Unsupported wearable source: " + source);
        }
        
        log.debug("Found adapter: {} for source: {}", adapter.get().getClass().getSimpleName(), source);
        return adapter.get();
    }

    /**
     * 지원되는 모든 웨어러블 소스를 반환합니다.
     * 
     * @return 지원되는 웨어러블 소스 목록
     */
    @NonNull
    public List<WearableSource> getSupportedSources() {
        return adapters.stream()
                .flatMap(adapter -> 
                    java.util.Arrays.stream(WearableSource.values())
                            .filter(adapter::supports))
                .distinct()
                .toList();
    }

    /**
     * 특정 웨어러블 소스가 지원되는지 확인합니다.
     * 
     * @param source 확인할 웨어러블 소스
     * @return 지원 여부
     */
    public boolean isSupported(@NonNull WearableSource source) {
        return adapters.stream()
                .anyMatch(adapter -> adapter.supports(source));
    }

    /**
     * 등록된 어댑터 수를 반환합니다.
     * 
     * @return 어댑터 수
     */
    public int getAdapterCount() {
        return adapters.size();
    }
} 