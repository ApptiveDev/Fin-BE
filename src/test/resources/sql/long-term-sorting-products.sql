-- 목돈만들기 탭B 정렬 테스트용 상품 데이터
-- 상품명 오름차순, 잘못된 정렬 키(금리·환산수익률 기준) 모두 기대 순서와 다르도록 구성한다.
DELETE FROM product_property_keyword;
DELETE FROM product_property_required_keyword;
DELETE FROM product_properties;
DELETE FROM product;
DELETE FROM provider;

INSERT INTO provider (source_id, code, name) VALUES
((SELECT id FROM product_source WHERE code = 'FSS'), 'LONG_BANK', '테스트은행'),
((SELECT id FROM product_source WHERE code = 'ONTONG'), 'LONG_GOV', '테스트정책기관');

INSERT INTO product (source_id, type, product_code, product_name, content) VALUES
((SELECT id FROM product_source WHERE code = 'FSS'), 'SAVING', 'LONG_BANK_HIGH', 'B고금리적금', '연 4.0%'),
((SELECT id FROM product_source WHERE code = 'FSS'), 'SAVING', 'LONG_BANK_LOW', 'A저금리적금', '연 2.0%'),
((SELECT id FROM product_source WHERE code = 'ONTONG'), 'POLICY', 'LONG_GOV_36M', 'B장기매칭', '36개월 1:1 매칭'),
((SELECT id FROM product_source WHERE code = 'ONTONG'), 'POLICY', 'LONG_GOV_12M', 'A단기매칭', '12개월 1:1 매칭');

-- 정부: 36개월 매칭은 기여금 총액이 크고(36×월납입) 환산수익률이 낮다(33.3%),
--       12개월 매칭은 기여금 총액이 작고(12×월납입) 환산수익률이 높다(100%).
INSERT INTO product_properties (
    product_id, provider_id, base_rate, max_rate, min_monthly_limit, max_monthly_limit,
    gov_contribution_type, gov_matching_ratio, gov_monthly_fixed_contribution, gov_contribution_period_months,
    min_age, max_age, requires_homeless, requires_householder,
    is_joinable, intr_rate_type, save_trm
) VALUES
((SELECT id FROM product WHERE product_code = 'LONG_BANK_HIGH'), (SELECT id FROM provider WHERE code = 'LONG_BANK'), 4.0, 4.0, NULL, NULL, NULL, NULL, NULL, NULL, 19, 34, false, false, true, 'SINGLE_INTEREST', 12),
((SELECT id FROM product WHERE product_code = 'LONG_BANK_LOW'), (SELECT id FROM provider WHERE code = 'LONG_BANK'), 2.0, 2.0, NULL, NULL, NULL, NULL, NULL, NULL, 19, 34, false, false, true, 'SINGLE_INTEREST', 12),
((SELECT id FROM product WHERE product_code = 'LONG_GOV_36M'), (SELECT id FROM provider WHERE code = 'LONG_GOV'), 0.0, 0.0, NULL, NULL, 'RATIO', 1.0000, NULL, 36, 19, 34, false, false, true, NULL, 36),
((SELECT id FROM product WHERE product_code = 'LONG_GOV_12M'), (SELECT id FROM provider WHERE code = 'LONG_GOV'), 0.0, 0.0, NULL, NULL, 'RATIO', 1.0000, NULL, 12, 19, 34, false, false, true, NULL, 12);
