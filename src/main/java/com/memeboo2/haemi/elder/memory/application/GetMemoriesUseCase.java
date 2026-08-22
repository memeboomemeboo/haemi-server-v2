package com.memeboo2.haemi.elder.memory.application;

import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMemoriesUseCase {

    private final ElderMemoryQuery elderMemoryQuery;

    public List<MemoryItem> execute(UUID elderId) {
        return elderMemoryQuery.listForElder(elderId);
    }
}
