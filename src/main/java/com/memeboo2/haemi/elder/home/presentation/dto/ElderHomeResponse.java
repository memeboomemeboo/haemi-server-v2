package com.memeboo2.haemi.elder.home.presentation.dto;

import com.memeboo2.haemi.elder.home.application.GetElderHomeUseCase.ElderHomeData;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;

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
        static RecentMemory from(MemoryItem m, MediaUploadCommand mediaUploadCommand) {
            String img = m.imageKeys().isEmpty() ? null : m.imageKeys().get(0);
            if (mediaUploadCommand != null) img = mediaUploadCommand.resolveServingUrl(img);
            return new RecentMemory(m.id(), m.title(), img, m.responded());
        }
    }

    public record Training(boolean completedToday, int streak) {}

    public static ElderHomeResponse from(ElderHomeData data) {
        return from(data, null);
    }

    public static ElderHomeResponse from(ElderHomeData data, MediaUploadCommand mediaUploadCommand) {
        return new ElderHomeResponse(
                new GreetingSummary(data.todayGreetingCount(), data.unreadGreetingCount()),
                data.recentMemories().stream().map(memory -> RecentMemory.from(memory, mediaUploadCommand)).toList(),
                new Training(data.trainedToday(), data.streak())
        );
    }
}
