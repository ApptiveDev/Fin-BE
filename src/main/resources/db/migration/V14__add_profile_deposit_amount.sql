-- 마이페이지 대분류별 저축 목표 중 단기예치 예치 희망액을 저장한다.
-- 목돈만들기 금액은 기존 monthly_savings_goal을 그대로 쓴다. 단위도 그 컬럼과 같다.
ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS deposit_amount BIGINT;
