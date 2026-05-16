package org.backendcompas.modules.ai.service;

import org.backendcompas.modules.ai.dto.ChatMessageDto;
import org.backendcompas.modules.ai.dto.ChatRequestDto;
import org.backendcompas.modules.ai.dto.ChatResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AiChatService {

    ChatResponseDto chat(UUID userId, ChatRequestDto request);

    Page<ChatMessageDto> getHistory(UUID userId, int limit);
}
