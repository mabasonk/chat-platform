package com.jse.chat.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "A chat message")
public record ChatMessage(

        @Schema(description = "Display name of the sender", example = "Nkosi")
        String username,

        @Schema(description = "Message content", example = "Hello everyone!")
        String content,

        @Schema(description = "UTC timestamp when the message was created", example = "2026-06-28T10:15:30Z")
        Instant timestamp
) {
    public static ChatMessage of(String username, String content) {
        return new ChatMessage(username, content, Instant.now());
    }
}
