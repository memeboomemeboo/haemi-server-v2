-- #100 X3: 음성 답변 전사(STT) 텍스트 저장 컬럼
-- 황정빈 대역: V100~V199

ALTER TABLE elder_responses ADD COLUMN transcript VARCHAR(1000);
