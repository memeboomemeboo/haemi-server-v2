package com.memeboo2.haemi.elder.home.presentation.dto;

import com.memeboo2.haemi.elder.home.application.GetElderHomeUseCase.ElderHomeData;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;

import java.util.List;

public record ElderHomeResponse(
        GreetingSummary greeting,
        List<RecentMemory> recentMemories,
        Training training
) {
    public record GreetingSummary(long totalToday, long unread) {}

    public record RecentMemory(
            java.util.UUID id,
            String title,
            String firstImageKey,
            boolean responded
    ) {
        static RecentMemory from(MemoryItem m) {
            String img = m.imageKeys().isEmpty() ? null : m.imageKeys().get(0);
            return new RecentMemory(m.id(), m.title(), img, m.responded());
        }
    }

    public record Training(boolean completedToday, int streak) {}

    public static ElderHomeResponse from(ElderHomeData data) {
        return new ElderHomeResponse(
                new GreetingSummary(data.todayGreetingCount(), data.unreadGreetingCount()),
                data.recentMemories().stream().map(RecentMemory::from).toList(),
                new Training(data.trainedToday(), data.streak())
        );
    }
}
