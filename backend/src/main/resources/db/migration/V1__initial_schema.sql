-- =====================================================
-- SleepWell Backend - Initial Database Schema
-- Target: AWS RDS MySQL 8.0
-- Created: 2024-12-31
-- Author: SleepWell Development Team
-- =====================================================

-- Set MySQL specific settings for optimal performance
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION';

-- =====================================================
-- Core User Management Tables
-- =====================================================

-- Users table - Central user management
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    age INT,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    password VARCHAR(255),
    role ENUM('ADMIN', 'USER', 'PREMIUM_USER') NOT NULL DEFAULT 'USER',
    social_provider ENUM('GOOGLE', 'KAKAO', 'NAVER'),
    social_id VARCHAR(255),
    profile_image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at DATETIME(6),
    deleted_at DATETIME(6),
    fcm_token TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for users table (based on JPA @Index annotations)
CREATE UNIQUE INDEX IDX_USER_SOCIAL_LOGIN ON users (social_provider, social_id);
CREATE INDEX IDX_USER_ACTIVE_STATUS ON users (is_active, created_at);
CREATE INDEX IDX_USER_MARKETING_ACTIVE ON users (marketing_consent, is_active);
CREATE INDEX IDX_USER_LAST_LOGIN ON users (last_login_at, is_active);
CREATE INDEX IDX_USER_NAME_SEARCH ON users (name, is_active);

-- =====================================================
-- Sleep Data Management Tables
-- =====================================================

-- Sleep Records table - Core sleep data
CREATE TABLE sleep_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sleep_start_time DATETIME(6) NOT NULL,
    sleep_end_time DATETIME(6) NOT NULL,
    total_sleep_minutes INT NOT NULL,
    deep_sleep_minutes INT,
    light_sleep_minutes INT,
    rem_sleep_minutes INT,
    wakeup_count INT DEFAULT 0,
    sleep_in_bed_minutes INT,
    sleep_awake_minutes INT,
    light_level DOUBLE,
    noise_level DOUBLE,
    temperature DOUBLE,
    humidity DOUBLE,
    heart_rate_data TEXT,
    respiratory_rate_data TEXT,
    snore_detected BOOLEAN DEFAULT FALSE,
    bruxism_detected BOOLEAN DEFAULT FALSE,
    sleep_talk_detected BOOLEAN DEFAULT FALSE,
    audio_data_path VARCHAR(500),
    wearable_source ENUM('APPLE_HEALTH', 'SAMSUNG_HEALTH', 'GOOGLE_FIT', 'FLUTTER_HEALTH', 'MANUAL', 'OTHER'),
    sleep_quality_score INT,
    user_satisfaction INT,
    record_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_SLEEP_RECORD_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add unique constraint and indexes for sleep_records
ALTER TABLE sleep_records ADD CONSTRAINT UK_SLEEP_RECORD_USER_DATE UNIQUE (user_id, record_date);
CREATE INDEX IDX_SLEEP_RECORD_USER_DATE ON sleep_records (user_id, record_date);
CREATE INDEX IDX_SLEEP_RECORD_DATE_RANGE ON sleep_records (record_date, user_id);
CREATE INDEX IDX_SLEEP_RECORD_QUALITY ON sleep_records (sleep_quality_score, record_date);
CREATE INDEX IDX_SLEEP_RECORD_TIME_ANALYSIS ON sleep_records (sleep_start_time, total_sleep_minutes);
CREATE INDEX IDX_SLEEP_RECORD_SOURCE ON sleep_records (wearable_source, record_date);
CREATE INDEX IDX_SLEEP_RECORD_ENVIRONMENT ON sleep_records (temperature, humidity, record_date);
CREATE INDEX IDX_SLEEP_RECORD_DISRUPTION ON sleep_records (snore_detected, bruxism_detected, record_date);

-- =====================================================
-- AI Analysis and Intelligence Tables
-- =====================================================

-- Sleep Analysis table - AI-powered analysis results
CREATE TABLE sleep_analyses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sleep_record_id BIGINT NOT NULL,
    analysis_date DATE NOT NULL,
    sleep_score INT NOT NULL,
    sleep_quality VARCHAR(20),
    overall_score INT,
    insights TEXT,
    details TEXT,
    ai_generated BOOLEAN DEFAULT FALSE,
    sleep_efficiency DOUBLE NOT NULL,
    sleep_pattern_analysis TEXT,
    environmental_analysis TEXT,
    disruption_analysis TEXT,
    recommendations TEXT NOT NULL,
    weekly_trends TEXT,
    monthly_trends TEXT,
    model_version VARCHAR(50),
    confidence_score DECIMAL(3,2),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_SLEEP_ANALYSIS_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT FK_SLEEP_ANALYSIS_SLEEP_RECORD FOREIGN KEY (sleep_record_id) REFERENCES sleep_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for sleep_analyses
CREATE INDEX IDX_ANALYSIS_USER_DATE ON sleep_analyses (user_id, analysis_date);
CREATE INDEX IDX_ANALYSIS_SCORE_DATE ON sleep_analyses (sleep_score, analysis_date);
CREATE INDEX IDX_ANALYSIS_SLEEP_RECORD ON sleep_analyses (sleep_record_id);
CREATE INDEX IDX_ANALYSIS_CONFIDENCE ON sleep_analyses (confidence_score, analysis_date);
CREATE INDEX IDX_ANALYSIS_DATE_RANGE ON sleep_analyses (analysis_date, user_id);

-- Analysis Execution Jobs table - Async job management
CREATE TABLE analysis_execution_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sleep_record_id BIGINT,
    platform_source ENUM('APPLE_HEALTH', 'SAMSUNG_HEALTH', 'GOOGLE_FIT', 'FLUTTER_HEALTH', 'MANUAL', 'OTHER') NOT NULL,
    platform_data_id VARCHAR(500) NOT NULL,
    status ENUM('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'QUEUED',
    priority INT NOT NULL DEFAULT 3,
    started_at DATETIME(6),
    completed_at DATETIME(6),
    processing_time_ms BIGINT,
    worker_id VARCHAR(100),
    job_config TEXT,
    analysis_result TEXT,
    confidence_score DECIMAL(3,2),
    data_quality_score INT,
    ai_consultation_triggered BOOLEAN DEFAULT FALSE,
    consultation_session_id BIGINT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message TEXT,
    metadata TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_ANALYSIS_EXECUTION_JOB_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT FK_ANALYSIS_EXECUTION_JOB_SLEEP_RECORD FOREIGN KEY (sleep_record_id) REFERENCES sleep_records (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for analysis_execution_jobs
CREATE INDEX IDX_ANALYSIS_EXECUTION_JOB_USER ON analysis_execution_jobs (user_id, created_at);
CREATE INDEX IDX_ANALYSIS_EXECUTION_JOB_QUEUE ON analysis_execution_jobs (status, priority, created_at);
CREATE INDEX IDX_ANALYSIS_EXECUTION_JOB_PLATFORM ON analysis_execution_jobs (platform_data_id);
CREATE INDEX IDX_ANALYSIS_EXECUTION_JOB_WORKER ON analysis_execution_jobs (worker_id, status);
CREATE INDEX IDX_ANALYSIS_EXECUTION_JOB_SLEEP_RECORD ON analysis_execution_jobs (sleep_record_id);

-- =====================================================
-- Audio and Voice Processing Tables
-- =====================================================

-- Sleep Audio Events table - Audio event metadata
CREATE TABLE sleep_audio_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sleep_record_id BIGINT NOT NULL,
    event_type ENUM('SNORING', 'BRUXISM', 'SLEEP_TALKING', 'ENVIRONMENTAL_NOISE', 'BREATHING_IRREGULARITY', 'OTHER') NOT NULL,
    event_date DATETIME(6) NOT NULL,
    event_start_time DATETIME(6) NOT NULL,
    event_end_time DATETIME(6),
    duration_seconds INT NOT NULL,
    intensity_level INT NOT NULL,
    decibel_level DECIMAL(5,2),
    confidence_score DECIMAL(3,2),
    quality_score INT,
    frequency_per_hour DECIMAL(5,2),
    sleep_stage_context VARCHAR(10),
    is_continuous BOOLEAN DEFAULT FALSE,
    has_pattern BOOLEAN DEFAULT FALSE,
    snoring_metadata TEXT,
    bruxism_metadata TEXT,
    sleep_talk_metadata TEXT,
    environmental_metadata TEXT,
    data_source ENUM('APPLE_HEALTH', 'SAMSUNG_HEALTH', 'GOOGLE_FIT', 'FLUTTER_HEALTH', 'MANUAL', 'OTHER') NOT NULL,
    platform_event_id VARCHAR(255),
    collected_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    processing_status VARCHAR(20) DEFAULT 'RAW',
    additional_metadata TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_AUDIO_EVENT_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT FK_AUDIO_EVENT_SLEEP_RECORD FOREIGN KEY (sleep_record_id) REFERENCES sleep_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for sleep_audio_events
CREATE INDEX IDX_AUDIO_EVENT_SLEEP_RECORD ON sleep_audio_events (sleep_record_id, event_type);
CREATE INDEX IDX_AUDIO_EVENT_USER_DATE ON sleep_audio_events (user_id, event_date, event_type);
CREATE INDEX IDX_AUDIO_EVENT_TYPE_STATS ON sleep_audio_events (event_type, intensity_level, event_date);
CREATE INDEX IDX_AUDIO_EVENT_TIME_PATTERN ON sleep_audio_events (event_start_time, event_type);
CREATE INDEX IDX_AUDIO_EVENT_SOURCE ON sleep_audio_events (data_source, event_date);
CREATE INDEX IDX_AUDIO_EVENT_HIGH_INTENSITY ON sleep_audio_events (intensity_level, event_type, event_date);

-- =====================================================
-- Consultation and Communication Tables
-- =====================================================

-- Consultation Sessions table - AI consultation sessions
CREATE TABLE consultation_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    consultation_time DATETIME(6) NOT NULL,
    end_time DATETIME(6),
    status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL,
    topic ENUM('INSOMNIA', 'SLEEP_APNEA', 'SNORING', 'SLEEP_SCHEDULE', 'SLEEP_QUALITY', 'GENERAL', 'OTHER') NOT NULL,
    session_type ENUM('SLEEP_ANALYSIS', 'SLEEP_IMPROVEMENT', 'GENERAL_CONSULTATION', 'EMERGENCY', 'FOLLOW_UP') NOT NULL,
    title VARCHAR(200),
    initial_description TEXT,
    voice_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    estimated_duration_minutes INT,
    total_duration_minutes INT,
    ai_quality_score INT,
    user_satisfaction_score INT,
    user_feedback TEXT,
    metadata JSON,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_CONSULTATION_SESSION_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for consultation_sessions
CREATE INDEX IDX_CONSULTATION_SESSION_USER_STATUS ON consultation_sessions (user_id, status);
CREATE INDEX IDX_CONSULTATION_SESSION_USER_DATE ON consultation_sessions (user_id, consultation_time);
CREATE INDEX IDX_CONSULTATION_SESSION_STATUS_TIME ON consultation_sessions (status, consultation_time);
CREATE INDEX IDX_CONSULTATION_SESSION_IN_PROGRESS ON consultation_sessions (status, consultation_time);
CREATE INDEX IDX_CONSULTATION_SESSION_TOPIC_STATUS ON consultation_sessions (topic, status);
CREATE INDEX IDX_CONSULTATION_SESSION_TYPE_TIME ON consultation_sessions (session_type, consultation_time);
CREATE INDEX IDX_CONSULTATION_SESSION_EMERGENCY ON consultation_sessions (session_type, status, consultation_time);
CREATE INDEX IDX_CONSULTATION_SESSION_SATISFACTION ON consultation_sessions (user_satisfaction_score, end_time);
CREATE INDEX IDX_CONSULTATION_SESSION_AI_QUALITY ON consultation_sessions (ai_quality_score, end_time);
CREATE INDEX IDX_CONSULTATION_SESSION_COMPLETED_TIME ON consultation_sessions (status, end_time);
CREATE INDEX IDX_CONSULTATION_SESSION_DURATION ON consultation_sessions (total_duration_minutes, status);
CREATE INDEX IDX_CONSULTATION_SESSION_DATE_STATS ON consultation_sessions (consultation_time, status);

-- Conversation Messages table - Individual messages in consultations
CREATE TABLE conversation_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    consultation_session_id BIGINT NOT NULL,
    user_id BIGINT,
    message_type ENUM('USER_VOICE', 'USER_TEXT', 'AI_RESPONSE', 'SYSTEM_MESSAGE') NOT NULL,
    content TEXT,
    voice_file_path VARCHAR(500),
    stt_result TEXT,
    stt_confidence_score DOUBLE,
    ai_response TEXT,
    ai_model VARCHAR(50),
    ai_voice_file_path VARCHAR(500),
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL,
    error_message TEXT,
    sent_at DATETIME(6),
    processed_at DATETIME(6),
    response_time_ms BIGINT,
    sentiment_analysis JSON,
    intent_recognition VARCHAR(100),
    metadata JSON,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_CONVERSATION_MESSAGE_SESSION FOREIGN KEY (consultation_session_id) REFERENCES consultation_sessions (id) ON DELETE CASCADE,
    CONSTRAINT FK_CONVERSATION_MESSAGE_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for conversation_messages
CREATE INDEX IDX_CONVERSATION_MESSAGE_SESSION_TIME ON conversation_messages (consultation_session_id, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_SESSION_TYPE ON conversation_messages (consultation_session_id, message_type, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_STATUS_TIME ON conversation_messages (status, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_PENDING ON conversation_messages (status, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_PROCESSING ON conversation_messages (status, processed_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_USER_TIME ON conversation_messages (user_id, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_STT_CONFIDENCE ON conversation_messages (stt_confidence_score, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_VOICE_FILES ON conversation_messages (voice_file_path, status);
CREATE INDEX IDX_CONVERSATION_MESSAGE_INTENT ON conversation_messages (intent_recognition, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_SENTIMENT ON conversation_messages (sentiment_analysis, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_RESPONSE_TIME ON conversation_messages (response_time_ms, message_type);
CREATE INDEX IDX_CONVERSATION_MESSAGE_AI_RESPONSE ON conversation_messages (message_type, response_time_ms);
CREATE INDEX IDX_CONVERSATION_MESSAGE_FAILED ON conversation_messages (status, error_message, created_at);
CREATE INDEX IDX_CONVERSATION_MESSAGE_DAILY_STATS ON conversation_messages (created_at, status, message_type);

-- Voice Processing Jobs table - Async voice processing
CREATE TABLE voice_processing_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    processing_type ENUM('STT', 'TTS', 'SENTIMENT_ANALYSIS', 'INTENT_RECOGNITION', 'VOICE_ENHANCEMENT') NOT NULL,
    status ENUM('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL,
    priority INT NOT NULL,
    input_path VARCHAR(500),
    output_path VARCHAR(500),
    result_data JSON,
    error_message TEXT,
    started_at DATETIME(6),
    completed_at DATETIME(6),
    processing_time_ms BIGINT,
    ai_model VARCHAR(100),
    confidence_score DOUBLE,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    job_config JSON,
    worker_id VARCHAR(100),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_VOICE_PROCESSING_JOB_MESSAGE FOREIGN KEY (message_id) REFERENCES conversation_messages (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for voice_processing_jobs
CREATE INDEX IDX_VOICE_PROCESSING_JOB_MESSAGE ON voice_processing_jobs (message_id, created_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_QUEUE ON voice_processing_jobs (status, priority, created_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_QUEUED ON voice_processing_jobs (status, priority, created_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_TYPE_STATUS ON voice_processing_jobs (processing_type, status, priority);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_TYPE_QUEUE ON voice_processing_jobs (processing_type, status, created_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_WORKER ON voice_processing_jobs (worker_id, status, started_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_PROCESSING ON voice_processing_jobs (status, worker_id, started_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_RETRY ON voice_processing_jobs (status, retry_count, max_retries, priority);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_FAILED ON voice_processing_jobs (status, retry_count, created_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_MODEL ON voice_processing_jobs (ai_model, status, confidence_score);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_CONFIDENCE ON voice_processing_jobs (confidence_score, processing_type, completed_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_PERFORMANCE ON voice_processing_jobs (processing_time_ms, processing_type, status);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_COMPLETED ON voice_processing_jobs (status, completed_at, processing_time_ms);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_HIGH_PRIORITY ON voice_processing_jobs (priority, status, created_at);
CREATE INDEX IDX_VOICE_PROCESSING_JOB_DAILY_STATS ON voice_processing_jobs (created_at, status, processing_type);

-- Consultation Summaries table - AI-generated consultation summaries
CREATE TABLE consultation_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    consultation_session_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    summary_text TEXT NOT NULL,
    main_summary TEXT,
    key_topics JSON,
    recommendations JSON,
    identified_issues JSON,
    suggested_solutions JSON,
    follow_up_plan JSON,
    next_consultation_recommended DATETIME(6),
    risk_assessment INT,
    confidence_score DOUBLE,
    emotion_summary JSON,
    effectiveness_score INT,
    engagement_score INT,
    ai_model VARCHAR(100),
    generated_at DATETIME(6),
    summary_rating INT,
    medical_referral VARCHAR(200),
    additional_notes TEXT,
    metadata JSON,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_CONSULTATION_SUMMARY_SESSION FOREIGN KEY (consultation_session_id) REFERENCES consultation_sessions (id) ON DELETE CASCADE,
    CONSTRAINT FK_CONSULTATION_SUMMARY_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for consultation_summaries
CREATE INDEX IDX_CONSULTATION_SUMMARY_SESSION ON consultation_summaries (consultation_session_id);
CREATE INDEX IDX_CONSULTATION_SUMMARY_USER_TIME ON consultation_summaries (user_id, created_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_RATING ON consultation_summaries (summary_rating, created_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_HIGH_RATING ON consultation_summaries (summary_rating, generated_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_RISK ON consultation_summaries (risk_assessment, created_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_HIGH_RISK ON consultation_summaries (risk_assessment, medical_referral, created_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_MEDICAL_REFERRAL ON consultation_summaries (medical_referral, risk_assessment);
CREATE INDEX IDX_CONSULTATION_SUMMARY_EFFECTIVENESS ON consultation_summaries (effectiveness_score, created_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_ENGAGEMENT ON consultation_summaries (engagement_score, created_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_PERFORMANCE ON consultation_summaries (effectiveness_score, engagement_score);
CREATE INDEX IDX_CONSULTATION_SUMMARY_AI_MODEL ON consultation_summaries (ai_model, confidence_score, generated_at);
CREATE INDEX IDX_CONSULTATION_SUMMARY_CONFIDENCE ON consultation_summaries (confidence_score, ai_model);
CREATE INDEX IDX_CONSULTATION_SUMMARY_FOLLOW_UP ON consultation_summaries (next_consultation_recommended, risk_assessment);
CREATE INDEX IDX_CONSULTATION_SUMMARY_MONTHLY_STATS ON consultation_summaries (generated_at, summary_rating, effectiveness_score);
CREATE INDEX IDX_CONSULTATION_SUMMARY_DAILY_STATS ON consultation_summaries (created_at, risk_assessment, medical_referral);
CREATE INDEX IDX_CONSULTATION_SUMMARY_USER_TREND ON consultation_summaries (user_id, generated_at, effectiveness_score);

-- =====================================================
-- Notification and Communication Tables
-- =====================================================

-- Notification Templates table - Template-based notifications
CREATE TABLE notification_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    type ENUM('SLEEP_REMINDER', 'ANALYSIS_COMPLETE', 'CONSULTATION_SCHEDULED', 'HEALTH_ALERT', 'MARKETING', 'SYSTEM', 'OTHER') NOT NULL,
    category VARCHAR(100) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'ko',
    title_template VARCHAR(500) NOT NULL,
    message_template TEXT NOT NULL,
    default_priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') NOT NULL DEFAULT 'NORMAL',
    default_expiry_hours INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version VARCHAR(20) NOT NULL DEFAULT '1.0',
    usage_count BIGINT NOT NULL DEFAULT 0,
    last_used_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for notification_templates
CREATE INDEX idx_template_type_locale ON notification_templates (type, locale);
CREATE INDEX idx_template_active ON notification_templates (is_active);
CREATE INDEX idx_template_category ON notification_templates (category);

-- Notifications table - User notifications
CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('SLEEP_REMINDER', 'ANALYSIS_COMPLETE', 'CONSULTATION_SCHEDULED', 'HEALTH_ALERT', 'MARKETING', 'SYSTEM', 'OTHER') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME(6),
    scheduled_at DATETIME(6),
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at DATETIME(6),
    is_push_sent BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at DATETIME(6),
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') NOT NULL DEFAULT 'NORMAL',
    related_data_id BIGINT,
    related_data_type VARCHAR(100),
    deep_link_url VARCHAR(500),
    action_buttons TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_NOTIFICATION_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for notifications
CREATE INDEX idx_notification_user_created_at ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notification_scheduled_at ON notifications (scheduled_at, is_sent);
CREATE INDEX idx_notification_expires_at ON notifications (expires_at);
CREATE INDEX idx_notification_push_pending ON notifications (is_push_sent, is_sent);

-- =====================================================
-- Subscription and Business Logic Tables
-- =====================================================

-- Subscriptions table - User subscription management
CREATE TABLE subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_type ENUM('FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE') NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'CANCELLED', 'EXPIRED', 'PENDING') NOT NULL,
    start_date DATETIME(6) NOT NULL,
    end_date DATETIME(6) NOT NULL,
    next_billing_date DATETIME(6),
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method ENUM('CREDIT_CARD', 'BANK_TRANSFER', 'PAYPAL', 'GOOGLE_PAY', 'APPLE_PAY', 'KAKAO_PAY', 'OTHER'),
    external_payment_id VARCHAR(100),
    auto_renewal BOOLEAN NOT NULL DEFAULT TRUE,
    coupon_code VARCHAR(50),
    discount_amount DECIMAL(10,2),
    cancelled_at DATETIME(6),
    cancellation_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_SUBSCRIPTION_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for subscriptions
CREATE INDEX IDX_SUBSCRIPTION_USER_STATUS ON subscriptions (user_id, status);
CREATE INDEX IDX_SUBSCRIPTION_AUTO_RENEWAL ON subscriptions (auto_renewal, next_billing_date, status);
CREATE INDEX IDX_SUBSCRIPTION_EXPIRY ON subscriptions (end_date, status);
CREATE INDEX IDX_SUBSCRIPTION_PAYMENT ON subscriptions (external_payment_id, status);
CREATE INDEX IDX_SUBSCRIPTION_PLAN_ANALYSIS ON subscriptions (plan_type, status, start_date);
CREATE INDEX IDX_SUBSCRIPTION_CANCELLED ON subscriptions (cancelled_at, cancellation_reason);

-- =====================================================
-- Lifestyle and User Feedback Tables
-- =====================================================

-- Lifestyle Data table - User lifestyle patterns
CREATE TABLE lifestyle_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    exercise_type VARCHAR(50),
    exercise_intensity VARCHAR(20),
    exercise_duration_minutes INT,
    exercise_start_time DATETIME(6),
    daily_steps INT,
    active_calories INT,
    caffeine_intake INT DEFAULT 0,
    last_caffeine_time DATETIME(6),
    alcohol_intake INT DEFAULT 0,
    last_alcohol_time DATETIME(6),
    water_intake INT DEFAULT 0,
    dinner_time DATETIME(6),
    late_snack BOOLEAN DEFAULT FALSE,
    stress_level INT,
    mood_state VARCHAR(20),
    significant_event BOOLEAN DEFAULT FALSE,
    event_description VARCHAR(500),
    primary_location VARCHAR(20),
    weather_condition VARCHAR(20),
    outdoor_time_minutes INT,
    natural_light_exposure INT,
    screen_time_minutes INT,
    pre_bedtime_screen_time INT,
    blue_light_filter_used BOOLEAN DEFAULT FALSE,
    social_media_minutes INT,
    sleep_routine_followed BOOLEAN DEFAULT FALSE,
    sleep_routine_details TEXT,
    relaxation_activity VARCHAR(20),
    relaxation_duration_minutes INT,
    weight DOUBLE,
    body_temperature DOUBLE,
    notes TEXT,
    data_source VARCHAR(30) DEFAULT 'MANUAL',
    collected_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_LIFESTYLE_DATA_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for lifestyle_data
CREATE INDEX IDX_LIFESTYLE_USER_DATE ON lifestyle_data (user_id, record_date);
CREATE INDEX IDX_LIFESTYLE_DATE_RANGE ON lifestyle_data (record_date, user_id);
CREATE INDEX IDX_LIFESTYLE_EXERCISE ON lifestyle_data (exercise_type, exercise_intensity);
CREATE INDEX IDX_LIFESTYLE_CAFFEINE ON lifestyle_data (caffeine_intake, last_caffeine_time);
CREATE INDEX IDX_LIFESTYLE_STRESS ON lifestyle_data (stress_level, record_date);
CREATE INDEX IDX_LIFESTYLE_SCREEN_TIME ON lifestyle_data (screen_time_minutes, record_date);

-- Sleep Feedback table - User subjective feedback
CREATE TABLE sleep_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sleep_record_id BIGINT NOT NULL UNIQUE,
    feedback_date DATE NOT NULL,
    overall_satisfaction INT NOT NULL,
    fatigue_recovery INT NOT NULL,
    perceived_sleep_depth INT,
    perceived_sleep_latency INT,
    sleep_onset_difficulty INT,
    perceived_wakeup_count INT DEFAULT 0,
    back_to_sleep_difficulty INT,
    morning_freshness INT NOT NULL,
    morning_condition INT NOT NULL,
    daytime_sleepiness INT,
    concentration_level INT,
    mood_rating INT,
    temperature_comfort INT,
    noise_comfort INT,
    lighting_comfort INT,
    bedding_comfort INT,
    perceived_snoring BOOLEAN DEFAULT FALSE,
    partner_disturbance BOOLEAN DEFAULT FALSE,
    external_noise_disturbance BOOLEAN DEFAULT FALSE,
    stress_disturbance BOOLEAN DEFAULT FALSE,
    physical_discomfort BOOLEAN DEFAULT FALSE,
    sleep_notes TEXT,
    dream_recall TEXT,
    improvement_suggestions TEXT,
    comparison_with_previous TEXT,
    feedback_collected_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    collection_method VARCHAR(30) DEFAULT 'MORNING_SURVEY',
    completion_percentage INT,
    reliability_score INT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_SLEEP_FEEDBACK_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT FK_SLEEP_FEEDBACK_SLEEP_RECORD FOREIGN KEY (sleep_record_id) REFERENCES sleep_records (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for sleep_feedback
CREATE INDEX IDX_SLEEP_FEEDBACK_USER_DATE ON sleep_feedback (user_id, feedback_date);
CREATE INDEX IDX_SLEEP_FEEDBACK_RECORD ON sleep_feedback (sleep_record_id);
CREATE INDEX IDX_SLEEP_FEEDBACK_SATISFACTION ON sleep_feedback (overall_satisfaction, feedback_date);
CREATE INDEX IDX_SLEEP_FEEDBACK_CONDITION ON sleep_feedback (morning_condition, feedback_date);
CREATE INDEX IDX_SLEEP_FEEDBACK_TIMING ON sleep_feedback (feedback_collected_at, feedback_date);
CREATE INDEX IDX_SLEEP_FEEDBACK_RECOVERY ON sleep_feedback (fatigue_recovery, overall_satisfaction);

-- =====================================================
-- Trend Analysis and Statistics Tables
-- =====================================================

-- Sleep Trends table - Long-term pattern analysis
CREATE TABLE sleep_trends (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    trend_date DATE NOT NULL,
    trend_type VARCHAR(20) NOT NULL,
    period_days INT NOT NULL,
    average_sleep_minutes DECIMAL(8,2),
    sleep_minutes_std_dev DECIMAL(8,2),
    average_sleep_efficiency DECIMAL(5,2),
    sleep_efficiency_std_dev DECIMAL(5,2),
    average_bedtime_hour DECIMAL(4,2),
    bedtime_std_dev DECIMAL(4,2),
    average_wakeup_hour DECIMAL(4,2),
    wakeup_std_dev DECIMAL(4,2),
    average_sleep_score DECIMAL(5,2),
    sleep_score_std_dev DECIMAL(5,2),
    average_deep_sleep_ratio DECIMAL(5,2),
    average_rem_sleep_ratio DECIMAL(5,2),
    average_light_sleep_ratio DECIMAL(5,2),
    average_wakeup_count DECIMAL(4,2),
    trend_score INT,
    change_rate DECIMAL(6,2),
    trend_direction VARCHAR(20),
    consistency_score INT,
    cyclicity_detected BOOLEAN DEFAULT FALSE,
    cyclicity_pattern TEXT,
    anomaly_count INT DEFAULT 0,
    anomaly_distribution TEXT,
    anomaly_dates TEXT,
    weather_impact DECIMAL(3,2),
    seasonal_impact DECIMAL(3,2),
    lifestyle_impact DECIMAL(3,2),
    predicted_score INT,
    prediction_confidence INT,
    goal_achievability_percent INT,
    age_group_percentile INT,
    gender_group_percentile INT,
    overall_percentile INT,
    data_point_count INT,
    confidence_level VARCHAR(10),
    calculated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    next_update_date DATE,
    analysis_version VARCHAR(20) DEFAULT '1.0',
    algorithm_details TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT FK_SLEEP_TREND_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for sleep_trends
CREATE INDEX IDX_SLEEP_TREND_USER_DATE ON sleep_trends (user_id, trend_date);
CREATE INDEX IDX_SLEEP_TREND_TYPE_DATE ON sleep_trends (trend_type, trend_date);
CREATE INDEX IDX_SLEEP_TREND_PERIOD ON sleep_trends (period_days, calculated_at);
CREATE INDEX IDX_SLEEP_TREND_SCORE ON sleep_trends (trend_score, trend_type);
CREATE INDEX IDX_SLEEP_TREND_CHANGE_RATE ON sleep_trends (change_rate, trend_date);
CREATE INDEX IDX_SLEEP_TREND_CONFIDENCE ON sleep_trends (confidence_level, trend_date);

-- =====================================================
-- System Configuration and Optimization
-- =====================================================

-- Add foreign key constraint for consultation sessions -> analysis execution jobs
ALTER TABLE analysis_execution_jobs 
ADD CONSTRAINT FK_ANALYSIS_EXECUTION_JOB_CONSULTATION_SESSION 
FOREIGN KEY (consultation_session_id) REFERENCES consultation_sessions (id) ON DELETE SET NULL;

-- Set MySQL specific optimizations
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
SET SQL_MODE=@OLD_SQL_MODE;

-- Create views for commonly used queries (Optional - can be added in later migrations)

-- View for active users with recent sleep data
CREATE VIEW v_active_users_with_sleep_data AS
SELECT 
    u.id,
    u.name,
    u.email,
    u.created_at,
    u.last_login_at,
    COUNT(sr.id) as sleep_record_count,
    MAX(sr.record_date) as latest_sleep_record,
    AVG(sr.sleep_quality_score) as avg_sleep_quality
FROM users u
LEFT JOIN sleep_records sr ON u.id = sr.user_id 
    AND sr.record_date >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
WHERE u.is_active = TRUE
GROUP BY u.id, u.name, u.email, u.created_at, u.last_login_at;

-- View for user sleep summary (last 7 days)
CREATE VIEW v_user_sleep_summary_7d AS
SELECT 
    u.id as user_id,
    u.name,
    COUNT(sr.id) as sleep_records_count,
    AVG(sr.total_sleep_minutes) as avg_sleep_minutes,
    AVG(sr.sleep_quality_score) as avg_sleep_quality,
    AVG(sr.sleep_efficiency) as avg_sleep_efficiency,
    SUM(CASE WHEN sr.snore_detected = TRUE THEN 1 ELSE 0 END) as snoring_nights,
    MAX(sr.record_date) as latest_record_date
FROM users u
LEFT JOIN sleep_records sr ON u.id = sr.user_id 
    AND sr.record_date >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)
WHERE u.is_active = TRUE
GROUP BY u.id, u.name;

-- Optimize table storage and add partitioning for large tables (Future consideration)
-- Note: This would be implemented in separate migration files for production

-- =====================================================
-- Initial Data Seeding (Optional)
-- =====================================================

-- Insert default notification templates
INSERT INTO notification_templates (name, description, type, category, title_template, message_template, default_priority) VALUES
('SLEEP_REMINDER_BEDTIME', '취침 시간 알림 템플릿', 'SLEEP_REMINDER', 'sleep', '💤 취침 시간이에요!', '좋은 잠을 위해 {{name}}님, 이제 잠자리에 드실 시간입니다.', 'NORMAL'),
('ANALYSIS_COMPLETE_BASIC', '수면 분석 완료 알림', 'ANALYSIS_COMPLETE', 'sleep', '✨ 수면 분석이 완료되었어요', '{{date}}의 수면 분석이 완료되었습니다. 점수는 {{score}}점이에요!', 'NORMAL'),
('HEALTH_ALERT_HIGH_RISK', '건강 위험 알림', 'HEALTH_ALERT', 'health', '⚠️ 수면 건강 주의', '{{name}}님의 수면 패턴에서 주의가 필요한 부분이 발견되었습니다.', 'HIGH'),
('CONSULTATION_AVAILABLE', '상담 가능 알림', 'CONSULTATION_SCHEDULED', 'consultation', '🤖 AI 상담이 준비되었어요', 'AI 상담사가 {{name}}님의 수면 개선을 도와드릴 준비가 되었습니다.', 'NORMAL');

-- =====================================================
-- Performance and Monitoring Setup
-- =====================================================

-- Enable Query Cache (if supported by MySQL version)
-- SET GLOBAL query_cache_size = 268435456; -- 256MB
-- SET GLOBAL query_cache_type = 1;

-- Set appropriate InnoDB settings for performance
-- These would typically be set in MySQL configuration file
-- innodb_buffer_pool_size = 70% of available RAM
-- innodb_log_file_size = 256M
-- innodb_flush_log_at_trx_commit = 2
-- innodb_file_per_table = 1

-- =====================================================
-- Completion Notice
-- =====================================================

-- Schema creation completed successfully
-- Tables created: 15 core tables with proper relationships
-- Indexes created: Optimized for query performance
-- Foreign keys: Proper referential integrity
-- Storage engine: InnoDB for ACID compliance
-- Character set: utf8mb4 for full Unicode support
-- Collation: utf8mb4_unicode_ci for proper Korean text handling

SELECT 'SleepWell Backend Database Schema V1 - Created Successfully!' as status;