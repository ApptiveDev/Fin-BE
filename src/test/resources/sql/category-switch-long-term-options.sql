-- 대분류 전환 테스트용: short-term-sorting-products.sql 위에 목돈 기간(12개월) 옵션을 더한다.
-- 단기예치로 전환했을 때 12개월 옵션이 섞이지 않는지 확인한다.

-- 고금리예금에 12개월 옵션 추가 (같은 상품 안에서 실수령액이 더 커 1개월 옵션을 밀어내면 안 된다)
INSERT INTO product_properties (
    product_id, provider_id, base_rate, max_rate, min_monthly_limit, max_monthly_limit,
    min_age, max_age, is_joinable, intr_rate_type, save_trm
) VALUES
((SELECT id FROM product WHERE product_code = 'SHORT_HIGH_RATE'), (SELECT id FROM provider WHERE code = 'SHORT_BANK_A'), 4.0, 4.5, NULL, NULL, 17, 100, true, 'SINGLE_INTEREST', 12);

-- 12개월 옵션만 있는 예금 (단기 결과에 나오면 안 된다)
INSERT INTO product (source_id, type, product_code, product_name, content) VALUES
((SELECT id FROM product_source WHERE code = 'FSS'), 'DEPOSIT', 'LONG_ONLY', '1년예금', '12개월 상품');

INSERT INTO product_properties (
    product_id, provider_id, base_rate, max_rate, min_monthly_limit, max_monthly_limit,
    min_age, max_age, is_joinable, intr_rate_type, save_trm
) VALUES
((SELECT id FROM product WHERE product_code = 'LONG_ONLY'), (SELECT id FROM provider WHERE code = 'SHORT_BANK_B'), 5.0, 5.5, NULL, NULL, 17, 100, true, 'SINGLE_INTEREST', 12);
