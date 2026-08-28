-- design-api-spec B4: 음성 답변 카드의 재생 시간 표시
ALTER TABLE elder_responses ADD COLUMN duration_seconds INT;
