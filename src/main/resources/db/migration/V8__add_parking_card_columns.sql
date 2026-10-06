-- 파킹통장 카드에 표시할 최고금리 적용 범위와 이자지급방식 컬럼을 추가한다.
-- (구간 금리 전체를 구조화하기 전의 표시용 값. Fin-API #15, #14)
--
-- max_rate_applicable_min/max_amount: 최고금리가 적용되는 잔액 범위(원).
--   하한은 초과, 상한은 이하로 본다. 예) 1천만원 초과 ~ 1억원 이하 구간이 최고 → (10000000, 100000000)
--   "5천만원 이하 우대" → (NULL, 50000000), "1억원 초과 구간이 최고" → (100000000, NULL)
--   둘 다 NULL이면 범위 정보 없음(단일 금리, 예치기간 구간 등).
-- interest_payment_method: 공시의 이자지급방식 원문(예: "월지급", "수시지급,월지급").
--
-- V6의 min/max_deposit_amount(예치 한도)는 예금과 파킹이 같은 의미로 함께 쓴다.
-- (V6 파일 주석을 고치면 이미 적용된 DB에서 checksum이 달라지므로 여기에 적는다.)
ALTER TABLE product_properties
    ADD COLUMN IF NOT EXISTS max_rate_applicable_min_amount BIGINT,
    ADD COLUMN IF NOT EXISTS max_rate_applicable_max_amount BIGINT,
    ADD COLUMN IF NOT EXISTS interest_payment_method VARCHAR(255);
