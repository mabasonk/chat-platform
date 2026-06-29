package com.jse.chat.messaging;

import com.jse.chat.model.ChatMessage;
import com.jse.chat.service.ChatService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
    private static final String TOPIC = "/topic/messages";

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageConsumer(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void startConsuming() {
        Thread consumerThread = new Thread(this::consumeMessages, "message-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void consumeMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ChatMessage message = chatService.getQueue().take();
                chatService.addToHistory(message);
                messagingTemplate.convertAndSend(TOPIC, message);
                log.info("Broadcast message from {}", message.username());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Message consumer interrupted, shutting down.");
            }
        }
    }
}
