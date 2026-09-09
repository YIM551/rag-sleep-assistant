package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * ASMR 파일 스토리지 서비스
 * AWS S3 연동 및 파일 관리
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ASMRFileStorageService {

    @Value("${spring.cloud.aws.s3.bucket:sleepwell-asmr}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static:ap-northeast-2}")
    private String region;

    private final S3Template s3Template;
    private final S3Presigner s3Presigner;

    /**
     * ASMR 오디오 파일 업로드
     */
    public Mono<String> uploadAudioFile(MultipartFile file, String category) {
        validateAudioFile(file);

        String fileName = generateFileName(file, category);

        return uploadToS3(file, fileName)
                .doOnSuccess(url -> log.info("ASMR 파일 업로드 성공: {}", url))
                .doOnError(error -> log.error("ASMR 파일 업로드 실패: {}", error.getMessage(), error));
    }

    /**
     * 다중 품질 파일 업로드 (고품질, 중품질, 저품질)
     */
    public Mono<Map<String, String>> uploadMultiQualityFiles(MultipartFile originalFile, String category) {
        validateAudioFile(originalFile);

        String baseFileName = generateBaseFileName(category);

        // 실제 구현에서는 외부 인코딩 서비스 API 호출
        return processMultipleQuality(originalFile, baseFileName)
                .doOnSuccess(urls -> log.info("다중 품질 파일 업로드 완료: {}", urls.size()))
                .doOnError(error -> log.error("다중 품질 파일 업로드 실패: {}", error.getMessage()));
    }

    /**
     * 파일 삭제
     */
    public Mono<Void> deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty() || !fileUrl.contains(bucketName)) {
            return Mono.error(new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 파일 URL입니다"));
        }

        String key = extractKeyFromUrl(fileUrl);
        if (key == null || key.trim().isEmpty()) {
            return Mono.error(new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 파일 URL입니다"));
        }
        return deleteFromS3(key)
                .doOnSuccess(unused -> log.info("ASMR 파일 삭제 완료: {}", key))
                .doOnError(error -> log.error("ASMR 파일 삭제 실패: {}", error.getMessage()));
    }

    /**
     * 파일 URL 생성 (CDN 포함)
     */
    public String generateCdnUrl(String s3Key) {
        return String.format("https://cdn.sleepwell.com/%s", s3Key);
    }

    /**
     * Presigned URL 생성 (12시간 유효)
     * Private S3 버킷의 파일에 일시적으로 접근할 수 있는 URL 생성
     * 수면 중 장시간 재생을 고려하여 12시간 유효 기간 설정
     */
    public String generatePresignedUrl(String s3Url) {
        try {
            // S3 URL에서 키 추출
            String key = extractKeyFromUrl(s3Url);

            // GetObject 요청 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Presigned URL 요청 생성 (12시간 유효 - 수면 시간 고려)
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(12))
                    .getObjectRequest(getObjectRequest)
                    .build();

            // Presigned URL 생성
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.debug("Presigned URL 생성: key={}, expires=12h", key);
            return presignedUrl;

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: url={}, error={}", s3Url, e.getMessage(), e);
            // 실패 시 원본 URL 반환 (fallback)
            return s3Url;
        }
    }

    /**
     * 오디오 파일 검증
     */
    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "파일이 없습니다");
        }

        // 파일 크기 제한 (100MB)
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.ASMR_FILE_SIZE_EXCEEDED);
        }

        // 파일 크기가 0이면 손상된 파일
        if (file.getSize() == 0) {
            throw new BusinessException(ErrorCode.ASMR_FILE_CORRUPTED, "파일 크기가 0입니다");
        }

        // 파일명 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "파일명이 없습니다");
        }

        // 파일명에 위험한 문자가 있는지 검증
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "안전하지 않은 파일명입니다");
        }

        // MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !isValidAudioType(contentType)) {
            throw new BusinessException(ErrorCode.ASMR_UNSUPPORTED_FILE_TYPE);
        }

        // 파일 확장자와 MIME 타입 일치 검증
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!isValidExtensionForMimeType(contentType, extension)) {
            throw new BusinessException(ErrorCode.ASMR_FILE_CORRUPTED, "파일 확장자와 내용이 일치하지 않습니다");
        }
    }

    /**
     * 지원되는 오디오 타입 확인
     */
    private boolean isValidAudioType(String contentType) {
        return contentType.equals("audio/mpeg") ||
               contentType.equals("audio/mp3") ||
               contentType.equals("audio/wav") ||
               contentType.equals("audio/m4a") ||
               contentType.equals("audio/aac");
    }

    /**
     * 고유한 파일명 생성
     */
    private String generateFileName(MultipartFile file, String category) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(file.getOriginalFilename());

        return String.format("asmr/%s/%s_%s%s", category.toLowerCase(), timestamp, uuid, extension);
    }

    /**
     * 기본 파일명 생성 (다중 품질용)
     */
    private String generateBaseFileName(String category) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("asmr/%s/%s_%s", category.toLowerCase(), timestamp, uuid);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".mp3";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 파일 확장자와 MIME 타입 일치 검증
     */
    private boolean isValidExtensionForMimeType(String contentType, String extension) {
        return switch (contentType.toLowerCase()) {
            case "audio/mpeg", "audio/mp3" -> extension.equals(".mp3");
            case "audio/wav" -> extension.equals(".wav");
            case "audio/m4a", "audio/aac" -> extension.equals(".m4a") || extension.equals(".aac");
            case "audio/ogg" -> extension.equals(".ogg");
            case "audio/flac" -> extension.equals(".flac");
            default -> false;
        };
    }

    /**
     * S3에 파일 업로드 (실제 Spring Cloud AWS 3.x S3Template 사용)
     */
    private Mono<String> uploadToS3(MultipartFile file, String key) {
        return Mono.fromCallable(() -> {
            try {
                // 파일 내용 한번 더 검증
                if (file.getBytes().length == 0) {
                    throw new BusinessException(ErrorCode.ASMR_FILE_CORRUPTED, "파일 내용이 비어있습니다");
                }

                // 실제 S3 업로드 (Spring Cloud AWS 3.x)
                s3Template.upload(bucketName, key, file.getInputStream());

                // 업로드된 파일 URL 반환
                String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);

                log.info("S3 업로드 성공: bucket={}, key={}, size={}bytes", bucketName, key, file.getSize());
                return url;

            } catch (BusinessException e) {
                throw e;
            } catch (IOException e) {
                log.error("파일 읽기 실패: key={}, error={}", key, e.getMessage(), e);
                throw new BusinessException(ErrorCode.ASMR_FILE_CORRUPTED.getMessage(),
                                          ErrorCode.ASMR_FILE_CORRUPTED.getHttpStatus(), e);
            } catch (Exception e) {
                log.error("S3 업로드 실패: key={}, error={}", key, e.getMessage(), e);
                throw new BusinessException(ErrorCode.ASMR_FILE_UPLOAD_FAILED.getMessage(),
                                          ErrorCode.ASMR_FILE_UPLOAD_FAILED.getHttpStatus(), e);
            }
        })
        .onErrorMap(throwable -> {
            if (throwable instanceof BusinessException) {
                return throwable;
            }
            return new BusinessException(ErrorCode.ASMR_FILE_UPLOAD_FAILED.getMessage(),
                                      ErrorCode.ASMR_FILE_UPLOAD_FAILED.getHttpStatus(), throwable);
        });
    }

    /**
     * 다중 품질 파일 처리 (원본 파일은 실제 업로드, 변환은 향후 구현)
     */
    private Mono<Map<String, String>> processMultipleQuality(MultipartFile originalFile, String baseFileName) {
        return Mono.fromCallable(() -> {
            try {
                // 원본 파일을 high quality로 실제 업로드
                String highQualityKey = baseFileName + "_high.mp3";
                s3Template.upload(bucketName, highQualityKey, originalFile.getInputStream());

                String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, baseFileName);

                // 실제 구현에서는 여기서 FFmpeg나 외부 인코딩 서비스 호출
                // 현재는 고품질 파일만 실제 업로드하고 나머지는 URL만 생성
                log.info("다중 품질 파일 처리: 원본 업로드 완료, 향후 인코딩 서비스 연동 필요");

                return Map.of(
                    "high", baseUrl + "_high.mp3",        // 실제 업로드됨
                    "medium", baseUrl + "_medium.mp3",    // 향후 구현
                    "low", baseUrl + "_low.mp3",          // 향후 구현
                    "preview", baseUrl + "_preview.mp3"   // 향후 구현
                );
            } catch (IOException e) {
                log.error("다중 품질 파일 처리 실패: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.ASMR_FILE_UPLOAD_FAILED.getMessage(),
                                          ErrorCode.ASMR_FILE_UPLOAD_FAILED.getHttpStatus(), e);
            }
        });
    }

    /**
     * S3에서 파일 삭제 (실제 S3Template 사용)
     */
    private Mono<Void> deleteFromS3(String key) {
        return Mono.fromRunnable(() -> {
            try {
                // 실제 S3 파일 삭제
                s3Template.deleteObject(bucketName, key);
                log.info("S3 파일 삭제 성공: bucket={}, key={}", bucketName, key);
            } catch (Exception e) {
                log.error("S3 파일 삭제 실패: bucket={}, key={}, error={}", bucketName, key, e.getMessage(), e);
                throw new BusinessException(ErrorCode.ASMR_FILE_UPLOAD_FAILED.getMessage(),
                                          ErrorCode.ASMR_FILE_UPLOAD_FAILED.getHttpStatus(), e);
            }
        });
    }

    /**
     * URL에서 S3 키 추출
     */
    private String extractKeyFromUrl(String url) {
        try {
            int comIndex = url.lastIndexOf(".com/");
            if (comIndex == -1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "URL에서 키를 추출할 수 없습니다");
            }
            return url.substring(comIndex + 5);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "URL에서 키를 추출할 수 없습니다");
        }
    }
}