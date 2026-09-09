-- V6: Sleep Diary, User, Sleep Stage 필드 추가
-- 작성일: 2024-12-14
-- 목적: 수면 일지 강화 및 사용자 프로필 확장, 수면 단계 상세 추적

-- =====================================================
-- 중요 사항:
-- 1. 프로덕션 환경에서는 Flyway가 비활성화되어 있음 (flyway.enabled=false)
-- 2. ddl-auto=update가 모든 환경에서 활성화되어 있음
-- 3. V1 스키마에 sleep_diary 테이블이 없음 (개발 환경에서만 JPA가 자동 생성)
-- 4. 이 마이그레이션은 수동 실행용 참고 자료로 작성됨
-- =====================================================

-- =====================================================
-- 1. User 테이블: occupation 필드 추가
-- =====================================================
-- V1 스키마에 이미 users 테이블이 존재하므로 ALTER TABLE 사용
ALTER TABLE users
ADD COLUMN IF NOT EXISTS occupation VARCHAR(100) COMMENT '직업';

-- occupation 인덱스 추가 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_user_occupation ON users(occupation);

-- =====================================================
-- 2. Sleep Diary 테이블 생성
-- =====================================================
-- V1 스키마에 sleep_diary 테이블이 없으므로 CREATE TABLE 필요
-- 개발 환경에서는 JPA ddl-auto=update로 자동 생성되지만,
-- 프로덕션 수동 마이그레이션을 위해 CREATE TABLE 스크립트 제공

CREATE TABLE IF NOT EXISTS sleep_diary (
    -- 기본 키
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 외래 키 (사용자)
    user_id BIGINT NOT NULL COMMENT '사용자 ID',

    -- 일지 날짜
    diary_date DATE NOT NULL COMMENT '일지 날짜',

    -- === 수치화 기록 (그래프화 가능) ===
    nap_minutes INT COMMENT '낮잠 시간 (분)',
    bed_time TIME COMMENT '침대에 누운 시간',
    perceived_sleep_minutes INT COMMENT '주관적 수면 시간 (분)',
    awakening_count INT COMMENT '각성 횟수',
    total_awake_minutes INT COMMENT '총 각성 시간 (분)',
    wake_up_time TIME COMMENT '기상 시간',
    caffeine_mg INT COMMENT '카페인 섭취량 (mg)',
    alcohol_ml INT COMMENT '알코올 섭취량 (ml)',
    medication_mg DOUBLE COMMENT '수면제 및 약물 복용량 (mg)',

    -- === 단순 기록 ===
    did_exercise BOOLEAN NOT NULL DEFAULT FALSE COMMENT '운동 여부',
    diary_notes TEXT COMMENT '자유 기재 일기',

    -- === 주관적 평가 ===
    subjective_sleep_quality INT COMMENT '주관적 수면 품질 점수 (1-10)',
    morning_condition_score INT COMMENT '아침 기상 시 컨디션 점수 (1-10)',
    stress_level INT COMMENT '스트레스 레벨 (1-10)',
    sleepiness INT COMMENT '주관적 졸림 정도 (1: 각성, 10: 졸림)',
    melatonin_intake_mg DOUBLE COMMENT '멜라토닌 섭취량 (mg)',

    -- === 감사 필드 (BaseEntity 패턴) ===
    created_by VARCHAR(100) COMMENT '생성자',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    modified_by VARCHAR(100) COMMENT '수정자',
    modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    -- === 제약 조건 ===

    -- 외래 키 제약 조건
    CONSTRAINT fk_sleep_diary_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- 유니크 제약 조건 (사용자당 하루 하나의 일지)
    CONSTRAINT uk_sleep_diary_user_date
        UNIQUE (user_id, diary_date),

    -- 체크 제약 조건 (도메인 규칙)
    CONSTRAINT chk_sleepiness_range
        CHECK (sleepiness IS NULL OR (sleepiness >= 1 AND sleepiness <= 10)),
    CONSTRAINT chk_melatonin_range
        CHECK (melatonin_intake_mg IS NULL OR (melatonin_intake_mg >= 0 AND melatonin_intake_mg <= 50)),
    CONSTRAINT chk_subjective_sleep_quality
        CHECK (subjective_sleep_quality IS NULL OR (subjective_sleep_quality >= 1 AND subjective_sleep_quality <= 10)),
    CONSTRAINT chk_morning_condition_score
        CHECK (morning_condition_score IS NULL OR (morning_condition_score >= 1 AND morning_condition_score <= 10)),
    CONSTRAINT chk_stress_level
        CHECK (stress_level IS NULL OR (stress_level >= 1 AND stress_level <= 10)),
    CONSTRAINT chk_nap_minutes
        CHECK (nap_minutes IS NULL OR nap_minutes >= 0),
    CONSTRAINT chk_perceived_sleep_minutes
        CHECK (perceived_sleep_minutes IS NULL OR perceived_sleep_minutes >= 0),
    CONSTRAINT chk_awakening_count
        CHECK (awakening_count IS NULL OR awakening_count >= 0),
    CONSTRAINT chk_total_awake_minutes
        CHECK (total_awake_minutes IS NULL OR total_awake_minutes >= 0),
    CONSTRAINT chk_caffeine_mg
        CHECK (caffeine_mg IS NULL OR caffeine_mg >= 0),
    CONSTRAINT chk_alcohol_ml
        CHECK (alcohol_ml IS NULL OR alcohol_ml >= 0),
    CONSTRAINT chk_medication_mg
        CHECK (medication_mg IS NULL OR medication_mg >= 0),

    -- === 인덱스 (성능 최적화) ===
    INDEX idx_sleep_diary_user_date (user_id, diary_date),
    INDEX idx_sleep_diary_date_range (diary_date, user_id),
    INDEX idx_sleep_diary_caffeine (caffeine_mg, diary_date),
    INDEX idx_sleep_diary_alcohol (alcohol_ml, diary_date),
    INDEX idx_sleep_diary_exercise (did_exercise, diary_date)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수면일지 (사용자 주관적 기록)';

-- =====================================================
-- 3. Sleep Stage 테이블 생성 (새 테이블)
-- =====================================================
-- V1 스키마에 sleep_stages 테이블이 없으므로 CREATE TABLE 사용
CREATE TABLE IF NOT EXISTS sleep_stages (
    -- 기본 키
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- 외래 키 (수면 기록)
    sleep_record_id BIGINT NOT NULL COMMENT '수면 기록 ID',

    -- 수면 단계 정보
    stage_type VARCHAR(20) NOT NULL COMMENT '수면 단계 타입 (DEEP, LIGHT, REM, AWAKE, UNKNOWN)',
    start_time DATETIME NOT NULL COMMENT '단계 시작 시간',
    end_time DATETIME NOT NULL COMMENT '단계 종료 시간',
    duration_minutes INT COMMENT '지속 시간 (분)',

    -- 데이터 품질
    confidence_score INT COMMENT '신뢰도 점수 (0-100)',
    device_source VARCHAR(100) COMMENT '데이터 수집 디바이스 (예: Apple Watch, Samsung Galaxy Watch)',
    raw_data_id VARCHAR(255) COMMENT '원본 데이터 식별자',

    -- 추가 메타데이터
    metadata TEXT COMMENT '추가 메타데이터 (JSON 형식)',

    -- 감사 필드 (BaseEntity 패턴)
    created_by VARCHAR(100) COMMENT '생성자',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    modified_by VARCHAR(100) COMMENT '수정자',
    modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    -- 외래 키 제약 조건
    CONSTRAINT fk_sleep_stage_sleep_record
        FOREIGN KEY (sleep_record_id)
        REFERENCES sleep_records(id)
        ON DELETE CASCADE,

    -- 체크 제약 조건
    CONSTRAINT chk_sleep_stage_time_order
        CHECK (start_time < end_time),
    CONSTRAINT chk_sleep_stage_confidence
        CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 100)),

    -- 인덱스
    INDEX idx_sleep_stage_record_id (sleep_record_id),
    INDEX idx_sleep_stage_type (stage_type),
    INDEX idx_sleep_stage_start_time (start_time),
    INDEX idx_sleep_stage_record_type (sleep_record_id, stage_type),
    INDEX idx_sleep_stage_record_time (sleep_record_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수면 단계 상세 정보 (웨어러블 기기 데이터)';

-- =====================================================
-- 4. 기존 데이터 호환성 보장
-- =====================================================
-- 기존 환경에서는 JPA ddl-auto=update로 이미 테이블이 생성되어 있을 수 있음
-- CREATE TABLE IF NOT EXISTS를 사용하여 안전하게 처리
-- ALTER TABLE IF NOT EXISTS를 사용하여 중복 실행 방지

-- =====================================================
-- 5. 롤백 스크립트 (참고용, 실행하지 않음)
-- =====================================================
-- ALTER TABLE users DROP COLUMN occupation;
-- DROP INDEX IF EXISTS idx_user_occupation ON users;
-- DROP TABLE IF EXISTS sleep_diary;
-- DROP TABLE IF EXISTS sleep_stages;

-- =====================================================
-- 6. 마이그레이션 실행 후 확인 쿼리
-- =====================================================
-- SELECT COUNT(*) FROM sleep_diary;
-- SELECT COUNT(*) FROM sleep_stages;
-- SHOW CREATE TABLE sleep_diary;
-- SHOW CREATE TABLE sleep_stages;
-- SHOW INDEXES FROM sleep_diary;
-- SHOW INDEXES FROM sleep_stages;
