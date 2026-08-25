package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.memory.domain.Memory;

public record MemoryWithCreator(
        Memory memory,
        String creatorName,
        GuardianRole creatorRole,
        boolean isMine
) {}
