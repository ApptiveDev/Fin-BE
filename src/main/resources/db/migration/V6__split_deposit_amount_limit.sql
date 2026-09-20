-- 예금 최대예치가능금액과 적금 월최대납입금액이 한 컬럼(max_monthly_limit)에 겸용되던 문제를 분리한다.
-- min/max_monthly_limit은 적금 전용(월 납입 한도)으로 의미를 확정하고,
-- 예금 전용 예치 한도 컬럼(min/max_deposit_amount)을 신설한다.
ALTER TABLE product_properties
    ADD COLUMN IF NOT EXISTS min_deposit_amount BIGINT,
    ADD COLUMN IF NOT EXISTS max_deposit_amount BIGINT;

-- 예금(DEPOSIT) 행의 기존 값을 예금 컬럼으로 이전한다.
UPDATE product_properties pp
SET min_deposit_amount = pp.min_monthly_limit,
    max_deposit_amount = pp.max_monthly_limit
FROM product p
WHERE pp.product_id = p.id
  AND p.type = 'DEPOSIT';

-- 이전 후 적금 컬럼에서 예금 값을 제거해 적금 전용으로 정리한다.
UPDATE product_properties pp
SET min_monthly_limit = NULL,
    max_monthly_limit = NULL
FROM product p
WHERE pp.product_id = p.id
  AND p.type = 'DEPOSIT';
