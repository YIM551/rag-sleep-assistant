package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.ASMRContentRequestDto;
import com.sleepwell.sleepwell_backend.dto.ASMRContentResponseDto;
import com.sleepwell.sleepwell_backend.entity.ASMRContent;
import com.sleepwell.sleepwell_backend.enums.ASMRCategory;
import com.sleepwell.sleepwell_backend.enums.ASMRContentStatus;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.ASMRContentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ASMR 콘텐츠 관리 서비스
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ASMRService {

    private final ASMRContentRepository asmrContentRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final ASMRFileStorageService fileStorageService;

    /**
     * ASMR 콘텐츠 목록 조회 (페이징)
     * Presigned URL 적용
     */
    @Transactional(readOnly = true)
    public Page<ASMRContentResponseDto> getActiveContents(Pageable pageable) {
        Page<ASMRContent> contents = asmrContentRepository.findByStatus(ASMRContentStatus.ACTIVE, pageable);
        return contents.map(this::convertToDtoWithPresignedUrl);
    }

    /**
     * 카테고리별 ASMR 콘텐츠 조회
     * Presigned URL 적용
     */
    @Transactional(readOnly = true)
    public Page<ASMRContentResponseDto> getContentsByCategory(ASMRCategory category, Pageable pageable) {
        Page<ASMRContent> contents = asmrContentRepository.findByCategoryAndStatus(category, ASMRContentStatus.ACTIVE, pageable);
        return contents.map(this::convertToDtoWithPresignedUrl);
    }

    /**
     * ASMR 콘텐츠 상세 조회
     * Presigned URL 적용
     */
    @Transactional(readOnly = true)
    public ASMRContentResponseDto getContent(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "콘텐츠 ID가 유효하지 않습니다");
        }

        ASMRContent content = asmrContentRepository.findByIdAndStatus(contentId, ASMRContentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASMR_CONTENT_NOT_FOUND));

        return convertToDtoWithPresignedUrl(content);
    }

    /**
     * 인기 ASMR 콘텐츠 목록
     * Presigned URL 적용
     */
    @Transactional(readOnly = true)
    public List<ASMRContentResponseDto> getPopularContents() {
        List<ASMRContent> contents = asmrContentRepository.findTop10ByStatusOrderByTotalPlayCountDesc(ASMRContentStatus.ACTIVE);
        return contents.stream()
                .map(this::convertToDtoWithPresignedUrl)
                .collect(Collectors.toList());
    }

    /**
     * ASMR 콘텐츠 생성 (관리자용)
     */
    public ASMRContentResponseDto createContent(ASMRContentRequestDto requestDto) {
        validateContentRequest(requestDto);

        try {
            ASMRContent content = ASMRContent.builder()
                    .title(requestDto.getTitle())
                    .description(requestDto.getDescription())
                    .category(requestDto.getCategory())
                    .durationMinutes(requestDto.getDurationMinutes())
                    .durationSeconds(requestDto.getDurationSeconds())
                    .audioQuality(requestDto.getAudioQuality())
                    .highQualityUrl(requestDto.getHighQualityUrl())
                    .mediumQualityUrl(requestDto.getMediumQualityUrl())
                    .lowQualityUrl(requestDto.getLowQualityUrl())
                    .previewUrl(requestDto.getPreviewUrl())
                    .thumbnailUrl(requestDto.getThumbnailUrl())
                    .tags(requestDto.getTags())
                    .targetMood(requestDto.getTargetMood())
                    .intensityLevel(requestDto.getIntensityLevel())
                    .creator(requestDto.getCreator())
                    .isPremium(requestDto.getIsPremium() != null ? requestDto.getIsPremium() : false)
                    .status(ASMRContentStatus.ACTIVE)
                    .build();

            ASMRContent savedContent = asmrContentRepository.save(content);
            log.info("ASMR 콘텐츠 생성: {}", savedContent.getTitle());

            return ASMRContentResponseDto.from(savedContent);
        } catch (Exception e) {
            log.error("ASMR 콘텐츠 생성 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.ASMR_CREATION_FAILED.getMessage(), ErrorCode.ASMR_CREATION_FAILED.getHttpStatus(), e);
        }
    }

    /**
     * ASMR 콘텐츠 재생 기록 (통계 업데이트)
     * 트랜잭션 격리: 실패 시 롤백되고 커넥션 즉시 반환
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordPlay(Long contentId, boolean completed) {
        if (contentId == null || contentId <= 0) {
            log.warn("잘못된 contentId로 재생 기록 시도: {}", contentId);
            throw new BusinessException(ErrorCode.ASMR_INVALID_PLAY_PARAMETERS, "유효하지 않은 콘텐츠 ID입니다");
        }

        ASMRContent content = asmrContentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASMR_CONTENT_NOT_FOUND));

        // 비활성화된 콘텐츠는 재생 기록하지 않음
        if (content.getStatus() != ASMRContentStatus.ACTIVE) {
            log.warn("비활성 콘텐츠 재생 시도: contentId={}, status={}", contentId, content.getStatus());
            throw new BusinessException(ErrorCode.ASMR_CONTENT_INACTIVE);
        }

        // try-catch 제거: 예외 발생 시 자동 롤백되도록
        content.updatePlayStats(completed);
        asmrContentRepository.save(content);
        log.info("ASMR 재생 기록 완료: contentId={}, completed={}, totalPlayCount={}",
                contentId, completed, content.getTotalPlayCount());
    }

    /**
     * ASMR 콘텐츠 삭제 (관리자용)
     * DB 레코드만 삭제하고 S3 파일은 유지됨 (중복 URL 참조 고려)
     * JdbcTemplate 사용으로 JPA/Hibernate 완전 우회
     */
    public void deleteContent(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "콘텐츠 ID가 유효하지 않습니다");
        }

        try {
            // JdbcTemplate으로 직접 삭제 (JPA/Hibernate 완전 우회)
            int deletedCount = jdbcTemplate.update(
                    "DELETE FROM asmr_content WHERE id = ?",
                    contentId);

            if (deletedCount == 0) {
                throw new BusinessException(ErrorCode.ASMR_CONTENT_NOT_FOUND);
            }

            log.info("ASMR 콘텐츠 삭제 완료: contentId={}, deletedCount={}", contentId, deletedCount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ASMR 콘텐츠 삭제 실패: contentId={}, error={}, type={}",
                    contentId, e.getMessage(), e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.ASMR_FILE_DELETE_FAILED.getMessage(),
                                      ErrorCode.ASMR_FILE_DELETE_FAILED.getHttpStatus(), e);
        }
    }

    /**
     * Entity를 DTO로 변환하면서 Presigned URL 적용
     */
    private ASMRContentResponseDto convertToDtoWithPresignedUrl(ASMRContent content) {
        ASMRContentResponseDto dto = ASMRContentResponseDto.from(content);

        // highQualityUrl에 Presigned URL 적용
        if (dto.getHighQualityUrl() != null && !dto.getHighQualityUrl().isEmpty()) {
            String presignedUrl = fileStorageService.generatePresignedUrl(dto.getHighQualityUrl());
            dto = dto.toBuilder().highQualityUrl(presignedUrl).build();
        }

        // mediumQualityUrl에 Presigned URL 적용 (있는 경우)
        if (dto.getMediumQualityUrl() != null && !dto.getMediumQualityUrl().isEmpty()) {
            String presignedUrl = fileStorageService.generatePresignedUrl(dto.getMediumQualityUrl());
            dto = dto.toBuilder().mediumQualityUrl(presignedUrl).build();
        }

        // lowQualityUrl에 Presigned URL 적용 (있는 경우)
        if (dto.getLowQualityUrl() != null && !dto.getLowQualityUrl().isEmpty()) {
            String presignedUrl = fileStorageService.generatePresignedUrl(dto.getLowQualityUrl());
            dto = dto.toBuilder().lowQualityUrl(presignedUrl).build();
        }

        return dto;
    }

    /**
     * 콘텐츠 생성 요청 검증
     */
    private void validateContentRequest(ASMRContentRequestDto requestDto) {
        if (requestDto == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "요청 데이터가 없습니다");
        }

        if (requestDto.getTitle() == null || requestDto.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "콘텐츠 제목은 필수입니다");
        }

        if (requestDto.getTitle().length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "콘텐츠 제목은 100자를 초과할 수 없습니다");
        }

        if (requestDto.getCategory() == null) {
            throw new BusinessException(ErrorCode.ASMR_INVALID_CATEGORY);
        }

        if (requestDto.getDurationMinutes() != null && (requestDto.getDurationMinutes() <= 0 || requestDto.getDurationMinutes() > 300)) {
            throw new BusinessException(ErrorCode.ASMR_INVALID_DURATION, "콘텐츠 길이는 1분 이상 300분 이하여야 합니다");
        }

        if (requestDto.getDurationSeconds() != null && (requestDto.getDurationSeconds() < 0 || requestDto.getDurationSeconds() >= 60)) {
            throw new BusinessException(ErrorCode.ASMR_INVALID_DURATION, "초는 0 이상 60 미만이어야 합니다");
        }
    }
}