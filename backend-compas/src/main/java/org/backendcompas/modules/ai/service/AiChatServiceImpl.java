package org.backendcompas.modules.ai.service;

import lombok.RequiredArgsConstructor;
import org.backendcompas.modules.ai.client.AiClient;
import org.backendcompas.modules.ai.client.dto.AiChatContext;
import org.backendcompas.modules.ai.client.dto.AiChatRequest;
import org.backendcompas.modules.ai.client.dto.AiChatResponse;
import org.backendcompas.modules.ai.client.dto.AiHistoryEntry;
import org.backendcompas.modules.ai.dto.ChatMessageDto;
import org.backendcompas.modules.ai.dto.ChatRequestDto;
import org.backendcompas.modules.ai.dto.ChatResponseDto;
import org.backendcompas.modules.ai.model.ChatMessage;
import org.backendcompas.modules.ai.model.ChatRole;
import org.backendcompas.modules.ai.repository.ChatMessageRepository;
import org.backendcompas.modules.budget.repository.BudgetPlanRepository;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final StudentProfileRepository profileRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final AiClient aiClient;

    @Override
    @Transactional
    public ChatResponseDto chat(UUID userId, ChatRequestDto request) {
        Object profileContext = profileRepository.findById(userId).orElse(null);
        Object planContext = budgetPlanRepository.findByUserId(userId).orElse(null);

        List<ChatMessage> recent = chatMessageRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        Collections.reverse(recent);
        List<AiHistoryEntry> history = recent.stream()
                .map(m -> new AiHistoryEntry(m.getRole().name(), m.getContent()))
                .toList();

        AiChatResponse aiResponse = aiClient.chat(
                new AiChatRequest(new AiChatContext(profileContext, planContext), history, request.message())
        );

        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setRole(ChatRole.USER);
        userMsg.setContent(request.message());
        chatMessageRepository.save(userMsg);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setRole(ChatRole.ASSISTANT);
        assistantMsg.setContent(aiResponse.reply());
        assistantMsg = chatMessageRepository.save(assistantMsg);

        return new ChatResponseDto(assistantMsg.getId(), aiResponse.reply());
    }

    @Override
    public Page<ChatMessageDto> getHistory(UUID userId, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        return chatMessageRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, pageSize))
                .map(ChatMessageDto::from);
    }
}
