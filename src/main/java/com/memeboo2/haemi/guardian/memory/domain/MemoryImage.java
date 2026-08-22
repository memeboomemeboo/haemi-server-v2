package com.memeboo2.haemi.guardian.memory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "guardian_memory_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "memory_id", nullable = false)
    private Memory memory;

    @Column(nullable = false, length = 500)
    private String storageKey;

    /** @OrderColumn 용 — JPA가 관리 */
    @Column(name = "position")
    private int position;

    static MemoryImage of(Memory memory, String storageKey) {
        MemoryImage img = new MemoryImage();
        img.memory = memory;
        img.storageKey = storageKey;
        return img;
    }
}
