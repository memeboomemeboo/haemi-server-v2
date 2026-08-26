-- #81 추억 이미지 SHA-256 중복 업로드 방지
-- 황정빈 대역: V100~V199

ALTER TABLE media_uploads ADD COLUMN content_hash VARCHAR(64);

-- 동일 업로더가 확정(CONFIRMED)한 동일 해시는 하나만 존재하도록 강제 (부분 유니크 인덱스).
-- PENDING/EXPIRED 및 해시 미제공(NULL)은 제약 대상에서 제외한다.
CREATE UNIQUE INDEX uq_media_uploads_uploader_hash
    ON media_uploads (uploader_id, content_hash)
    WHERE content_hash IS NOT NULL AND status = 'CONFIRMED';
