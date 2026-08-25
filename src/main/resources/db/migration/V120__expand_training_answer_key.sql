-- 제목(100자)과 콘텐츠 키워드(최대 500자)를 함께 보관하는 채점 키의 최대 길이를 수용한다.
ALTER TABLE elder_training_questions
    ALTER COLUMN answer_key TYPE VARCHAR(700);
