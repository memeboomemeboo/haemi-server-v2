-- Phase platform/media: presigned URL 기반 미디어 업로드 추적
-- 황정빈 대역: V100~V199

CREATE TABLE media_uploads (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    media_type              VARCHAR(30) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    storage_key             VARCHAR(500) NOT NULL,
    original_filename       VARCHAR(255) NOT NULL,
    content_type            VARCHAR(100) NOT NULL,
    declared_size_bytes     BIGINT      NOT NULL,
    uploader_id             UUID        NOT NULL,       -- FK 없음 (모듈 간 FK 금지)
    presigned_url_expires_at TIMESTAMPTZ NOT NULL,
    retain_until            TIMESTAMPTZ,               -- NULL = 무기한
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              UUID,
    deleted_at              TIMESTAMPTZ
);

CREATE INDEX idx_media_uploads_uploader_id ON media_uploads (uploader_id);
CREATE INDEX idx_media_uploads_status      ON media_uploads (status);
CREATE INDEX idx_media_uploads_retain_until ON media_uploads (retain_until) WHERE retain_until IS NOT NULL;
