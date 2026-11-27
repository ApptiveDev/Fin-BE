-- 토큰 검증 최적화를 위한 token_prefix 컬럼 추가
-- prefix로 먼저 조회 후 해시 비교하여 O(N) → O(1) 성능 개선

ALTER TABLE email_verification
    ADD COLUMN token_prefix VARCHAR(8);

-- 기존 데이터에 대해 token_prefix를 NULL로 유지 (만료되면 자연 삭제됨)
-- 새로운 토큰부터 prefix가 저장됨

-- NOT NULL 제약조건은 기존 데이터가 만료된 후 적용
-- ALTER TABLE email_verification ALTER COLUMN token_prefix SET NOT NULL;

-- 인덱스 추가
CREATE INDEX idx_email_verification_token_prefix ON email_verification(token_prefix);
