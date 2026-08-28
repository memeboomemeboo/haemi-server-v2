package com.memeboo2.haemi.guardian.memory.domain;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "guardian_memories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memory extends BaseEntity {

    private static final int MAX_IMAGES = 4;
    private static final int MAX_MEMO_LENGTH = 300;

    /** 대상 어르신 (FK 없음) */
    @Column(nullable = false)
    private UUID elderId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 300)
    private String memo;

    @Column(nullable = false, length = 100)
    private String message;

    @Column
    private Integer memoryYear;

    @Column(name = "memory_month")
    private Integer memoryMonth;

    @Column(length = 50)
    private String place;

    @OneToMany(mappedBy = "memory", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<MemoryImage> images = new ArrayList<>();

    /** 어르신 답변 여부 — elder/response 이벤트로 갱신 */
    @Column(nullable = false)
    private boolean responded = false;

    public static Memory create(UUID elderId, String title, String memo, String message, Integer memoryYear) {
        return create(elderId, title, memo, message, memoryYear, null, null);
    }

    public static Memory create(UUID elderId, String title, String memo, String message, Integer memoryYear,
                                Integer memoryMonth, String place) {
        if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "메모는 " + MAX_MEMO_LENGTH + "자 이하입니다.");
        }
        validateDateMetadata(memoryMonth, place);
        Memory m = new Memory();
        m.elderId = elderId;
        m.title = title;
        m.memo = memo;
        m.message = message;
        m.memoryYear = memoryYear;
        m.memoryMonth = memoryMonth;
        m.place = place;
        return m;
    }

    public void addImages(List<String> storageKeys) {
        if (images.size() + storageKeys.size() > MAX_IMAGES) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "추억 이미지는 최대 " + MAX_IMAGES + "장입니다.");
        }
        for (String key : storageKeys) {
            images.add(MemoryImage.of(this, key, images.size()));
        }
    }

    public void update(String title, String memo, String message, Integer memoryYear, List<String> storageKeys) {
        // 이전 시그니처에는 장소·월이 없었다. 호환 호출이 기존 메타데이터를 지우지 않도록 보존한다.
        update(title, memo, message, memoryYear, this.memoryMonth, this.place, storageKeys);
    }

    public void update(String title, String memo, String message, Integer memoryYear,
                       Integer memoryMonth, String place, List<String> storageKeys) {
        if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "메모는 " + MAX_MEMO_LENGTH + "자 이하입니다.");
        }
        validateDateMetadata(memoryMonth, place);
        this.title = title;
        this.memo = memo;
        this.message = message;
        this.memoryYear = memoryYear;
        this.memoryMonth = memoryMonth;
        this.place = place;
        this.images.clear();
        addImages(storageKeys);
    }

    private static void validateDateMetadata(Integer memoryMonth, String place) {
        if (memoryMonth != null && (memoryMonth < 1 || memoryMonth > 12)) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "추억 월은 1~12 사이여야 합니다.");
        }
        if (place != null && place.length() > 50) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "장소는 50자 이하입니다.");
        }
    }

    public void markResponded() {
        this.responded = true;
    }

    public void delete(Instant now) {
        softDelete(now);
    }

    public List<MemoryImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}
