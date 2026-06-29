package com.jse.chat.controller;

import com.jse.chat.model.ChatMessage;
import com.jse.chat.model.SendMessageRequest;
import com.jse.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "Send messages and retrieve chat history")
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(
            summary = "Send a chat message",
            description = "Enqueues a message for broadcast to all connected WebSocket clients. Returns 202 Accepted immediately; delivery is asynchronous.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Message accepted for delivery"),
                    @ApiResponse(responseCode = "400", description = "Validation failed — blank username/content or field exceeds max length",
                            content = @Content(schema = @Schema(implementation = Object.class)))
            }
    )
    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        ChatMessage message = ChatMessage.of(request.username(), request.content());
        chatService.enqueue(message);
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "Get chat history",
            description = "Returns all messages broadcast since the server started, in chronological order.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chat history retrieved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessage.class))))
            }
    )
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getHistory() {
        return ResponseEntity.ok(chatService.getHistory());
    }
}
