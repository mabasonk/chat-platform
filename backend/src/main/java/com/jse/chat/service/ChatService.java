package com.jse.chat.service;

import com.jse.chat.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ChatService {

    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();
    private final List<ChatMessage> history = Collections.synchronizedList(new ArrayList<>());

    public void enqueue(ChatMessage message) {
        messageQueue.offer(message);
    }

    public BlockingQueue<ChatMessage> getQueue() {
        return messageQueue;
    }

    public void addToHistory(ChatMessage message) {
        history.add(message);
    }

    public List<ChatMessage> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
