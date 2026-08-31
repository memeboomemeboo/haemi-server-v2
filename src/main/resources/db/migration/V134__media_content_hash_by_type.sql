-- content hash 중복 재사용은 MediaRef 용도별로만 허용한다.
-- 서로 다른 용도의 MediaRef를 재사용하면 소비 단계의 용도 검증과 충돌한다.

DROP INDEX uq_media_uploads_uploader_hash;

CREATE UNIQUE INDEX uq_media_uploads_uploader_type_hash
    ON media_uploads (uploader_id, media_type, content_hash)
    WHERE content_hash IS NOT NULL AND status = 'CONFIRMED';
