package com.memeboo2.haemi.elder.inbox;

import com.memeboo2.haemi.elder.inbox.presentation.dto.InboxItem;
import com.memeboo2.haemi.guardian.api.GreetingQuery.GreetingContent;
import com.memeboo2.haemi.guardian.api.GreetingQuery.ReceivedGreeting;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InboxItemTest {

    @Test
    void from_textGreeting_mapsTextFields() {
        UUID id = UUID.randomUUID();
        UUID guardianId = UUID.randomUUID();
        ReceivedGreeting greeting = new ReceivedGreeting(
                id, guardianId, "보호자", new GreetingContent.Text("오늘도 힘내세요"), true);

        InboxItem item = InboxItem.from(greeting);

        assertThat(item.id()).isEqualTo(id);
        assertThat(item.guardianId()).isEqualTo(guardianId);
        assertThat(item.type()).isEqualTo("TEXT");
        assertThat(item.text()).isEqualTo("오늘도 힘내세요");
        assertThat(item.mediaKey()).isNull();
        assertThat(item.durationSeconds()).isNull();
        assertThat(item.read()).isTrue();
    }

    @Test
    void from_voiceGreeting_mapsVoiceFields() {
        UUID id = UUID.randomUUID();
        UUID guardianId = UUID.randomUUID();
        ReceivedGreeting greeting = new ReceivedGreeting(
                id, guardianId, "보호자", new GreetingContent.Voice("media-key-1", 42), false);

        InboxItem item = InboxItem.from(greeting);

        assertThat(item.type()).isEqualTo("VOICE");
        assertThat(item.text()).isNull();
        assertThat(item.mediaKey()).isEqualTo("media-key-1");
        assertThat(item.durationSeconds()).isEqualTo(42);
        assertThat(item.read()).isFalse();
    }
}
