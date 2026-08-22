package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaRefRepository extends JpaRepository<MediaRef, UUID> {
}
