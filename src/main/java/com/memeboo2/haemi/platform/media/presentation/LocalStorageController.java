package com.memeboo2.haemi.platform.media.presentation;

import com.memeboo2.haemi.platform.media.infrastructure.LocalObjectStorage;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** LocalStorageAdapter가 발급하는 개발용 업로드·서빙 URL의 실제 처리기. */
@RestController
@RequestMapping("/internal/storage")
@Profile("!prod")
public class LocalStorageController {

    private final LocalObjectStorage objectStorage;

    public LocalStorageController(LocalObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    @PutMapping("/upload")
    public ResponseEntity<Void> upload(@RequestParam String key,
                                       @RequestBody byte[] content,
                                       @RequestParam(defaultValue = "application/octet-stream") String contentType,
                                       @RequestParam(required = false) Integer durationSeconds) {
        objectStorage.put(key, contentType, content, durationSeconds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/serve")
    public ResponseEntity<byte[]> serve(@RequestParam String key) {
        return objectStorage.get(key)
                .map(object -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(object.contentType()))
                        .body(object.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
