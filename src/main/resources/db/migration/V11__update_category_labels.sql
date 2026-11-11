-- V11: STEP1 카테고리/옵션 표시 문구를 기획 문구로 변경
-- value, name은 화면 표시에만 쓰이고 로직은 code와 id를 본다.
-- 숨김 처리(범위형 저축기간, 첫거래 고객, 재예치, 상품관심사)는 CategoryService 필터가 맡는다.

UPDATE category SET name = '핵심 혜택' WHERE id = 4;     -- 혜택선택
UPDATE category SET name = '은행 우대 조건' WHERE id = 6; -- 우대거래

UPDATE category_option SET value = '미취업' WHERE code = 'STATUS_UNEMPLOYED';
UPDATE category_option SET value = '알바/프리랜서' WHERE code = 'STATUS_PART_TIME';
UPDATE category_option SET value = '최고이율 중심' WHERE code = 'BENEFIT_MAX_INTEREST';
UPDATE category_option SET value = '자동이체 우대' WHERE code = 'BANK_AUTO_TRANSFER';
UPDATE category_option SET value = '카드실적 우대' WHERE code = 'BANK_CARD_USAGE';
UPDATE category_option SET value = '급여이체 우대' WHERE code = 'BANK_SALARY_TRANSFER';
UPDATE category_option SET value = '마케팅동의 OK' WHERE code = 'BANK_MARKETING';
