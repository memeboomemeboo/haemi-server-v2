package com.memeboo2.haemi.platform.media.infrastructure;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 개발 환경에서 presigned URL 흐름을 재현하는 메모리 기반 객체 저장소. */
@Component
public class LocalObjectStorage {

    private final ConcurrentHashMap<String, StoredObject> objects = new ConcurrentHashMap<>();

    public void put(String key, String contentType, byte[] content, Integer durationSeconds) {
        objects.put(key, new StoredObject(contentType, content.clone(), durationSeconds));
    }

    public Optional<StoredObject> get(String key) {
        return Optional.ofNullable(objects.get(key));
    }

    public record StoredObject(String contentType, byte[] content, Integer durationSeconds) {
        public StoredObject {
            content = content.clone();
        }
    }
}
