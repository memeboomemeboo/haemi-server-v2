-- design-api-spec B6: 추억 카드의 장소 및 연·월 표기를 위한 선택 메타데이터
ALTER TABLE guardian_memories
    ADD COLUMN memory_month INT,
    ADD COLUMN place VARCHAR(50),
    ADD CONSTRAINT chk_guardian_memories_memory_month
        CHECK (memory_month IS NULL OR memory_month BETWEEN 1 AND 12);
