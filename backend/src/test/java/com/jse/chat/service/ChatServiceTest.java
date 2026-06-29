package com.jse.chat.service;

import com.jse.chat.model.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatServiceTest {

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService();
    }

    @Test
    public void shouldEnqueueMessage() {

        ChatMessage message = ChatMessage.of("Nkosi", "Hello JSE!");

        chatService.enqueue(message);

        assertThat(chatService.getQueue()).hasSize(1);
        assertThat(chatService.getQueue().peek().username()).isEqualTo("Nkosi");
    }

    @Test
    public void shouldAddMessageToHistory() {

        ChatMessage message = ChatMessage.of("Nkosi", "Hello JSE!");

        chatService.addToHistory(message);

        assertThat(chatService.getHistory()).hasSize(1);
        assertThat(chatService.getHistory().get(0).content()).isEqualTo("Hello JSE!");
    }

    @Test
    public void shouldReturnUnmodifiableHistory() {

        chatService.addToHistory(ChatMessage.of("Nkosi", "Test"));
        assertThat(chatService.getHistory()).hasSize(1);
    }

    @Test
    public void shouldHandleMultipleMessages() {

        chatService.enqueue(ChatMessage.of("Nkosi", "Message 1"));
        chatService.enqueue(ChatMessage.of("Nkosi", "Message 2"));
        chatService.enqueue(ChatMessage.of("Nkosi", "Message 3"));

        assertThat(chatService.getQueue()).hasSize(3);
    }
}
