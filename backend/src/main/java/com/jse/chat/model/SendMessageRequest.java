package com.jse.chat.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for sending a chat message")
public record SendMessageRequest(

        @Schema(description = "Display name of the sender", example = "Nkosi", maxLength = 50)
        @NotBlank(message = "Username must not be blank")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        @Schema(description = "Message content", example = "Hello everyone!", maxLength = 1000)
        @NotBlank(message = "Content must not be blank")
        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        String content
) {}
