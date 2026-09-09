-- 알림 타입 컬럼 길이 확대
-- 배경: SUBSCRIPTION_EXPIRING_SOON (26자) 등 긴 enum 값 저장을 위해 VARCHAR(20) → VARCHAR(50)으로 확대
-- 이슈: Data truncated for column 'type' at row 1 오류 해결
-- 작성일: 2025-10-13

ALTER TABLE notifications
MODIFY COLUMN type VARCHAR(50) NOT NULL
COMMENT 'Notification type - extended to support longer enum values (max 26 chars)';
