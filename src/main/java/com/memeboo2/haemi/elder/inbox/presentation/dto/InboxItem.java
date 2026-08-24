package com.memeboo2.haemi.elder.inbox.presentation.dto;

import com.memeboo2.haemi.guardian.api.GreetingQuery.GreetingContent;
import com.memeboo2.haemi.guardian.api.GreetingQuery.ReceivedGreeting;

import java.util.UUID;

public record InboxItem(
        UUID id,
        UUID guardianId,
        String type,
        String text,
        String mediaKey,
        Integer durationSeconds,
        boolean read
) {
    public static InboxItem from(ReceivedGreeting g) {
        String type;
        String text = null;
        String mediaKey = null;
        Integer durationSeconds = null;

        if (g.content() instanceof GreetingContent.Text t) {
            type = "TEXT";
            text = t.message();
        } else if (g.content() instanceof GreetingContent.Voice v) {
            type = "VOICE";
            mediaKey = v.mediaKey();
            durationSeconds = v.durationSeconds();
        } else {
            type = "UNKNOWN";
        }

        return new InboxItem(g.id(), g.guardianId(), type, text, mediaKey, durationSeconds, g.read());
    }
}
