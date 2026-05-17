package org.backendcompas.modules.ai.service;

import org.backendcompas.modules.ai.client.AiClient;
import org.backendcompas.modules.ai.client.dto.AiChatRequest;
import org.backendcompas.modules.ai.client.dto.AiChatResponse;
import org.backendcompas.modules.ai.dto.ChatMessageDto;
import org.backendcompas.modules.ai.dto.ChatRequestDto;
import org.backendcompas.modules.ai.dto.ChatResponseDto;
import org.backendcompas.modules.ai.model.ChatMessage;
import org.backendcompas.modules.ai.model.ChatRole;
import org.backendcompas.modules.ai.repository.ChatMessageRepository;
import org.backendcompas.modules.budget.model.MonthlyBudget;
import org.backendcompas.modules.budget.repository.MonthlyBudgetRepository;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private MonthlyBudgetRepository monthlyBudgetRepository;

    @Mock
    private AiClient aiClient;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    @Test
    void chatWithContextAndHistory() {
        UUID userId = UUID.randomUUID();
        ChatRequestDto requestDto = new ChatRequestDto("How can I save?");
        
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(UUID.randomUUID());
        budget.setUserId(userId);
        
        ChatMessage oldMsg = new ChatMessage();
        oldMsg.setRole(ChatRole.USER);
        oldMsg.setContent("Hello");
        
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(monthlyBudgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.of(budget));
        when(chatMessageRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(oldMsg));
        
        when(aiClient.chat(any(AiChatRequest.class))).thenReturn(new AiChatResponse("Save 10%"));
        
        ChatMessage savedAssistantMsg = new ChatMessage();
        savedAssistantMsg.setId(UUID.randomUUID());
        savedAssistantMsg.setContent("Save 10%");
        savedAssistantMsg.setRole(ChatRole.ASSISTANT);
        
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            if (msg.getRole() == ChatRole.ASSISTANT) {
                return savedAssistantMsg;
            }
            return msg;
        });
        
        ChatResponseDto response = aiChatService.chat(userId, requestDto);
        
        assertThat(response.reply()).isEqualTo("Save 10%");
        assertThat(response.messageId()).isEqualTo(savedAssistantMsg.getId());
        
        verify(chatMessageRepository).findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void chatWithoutContext() {
        UUID userId = UUID.randomUUID();
        ChatRequestDto requestDto = new ChatRequestDto("No context");
        
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        when(monthlyBudgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(chatMessageRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());
        
        when(aiClient.chat(any(AiChatRequest.class))).thenReturn(new AiChatResponse("Reply without context"));
        
        ChatMessage savedAssistantMsg = new ChatMessage();
        savedAssistantMsg.setId(UUID.randomUUID());
        
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            if (msg.getRole() == ChatRole.ASSISTANT) {
                return savedAssistantMsg;
            }
            return msg;
        });
        
        ChatResponseDto response = aiChatService.chat(userId, requestDto);
        
        assertThat(response.reply()).isEqualTo("Reply without context");
    }

    @Test
    void getHistory() {
        UUID userId = UUID.randomUUID();
        
        ChatMessage msg1 = new ChatMessage();
        msg1.setId(UUID.randomUUID());
        msg1.setRole(ChatRole.USER);
        msg1.setContent("Message 1");
        msg1.setCreatedAt(Instant.now());
        
        Page<ChatMessage> page = new PageImpl<>(List.of(msg1));
        
        when(chatMessageRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(page);
                
        Page<ChatMessageDto> result = aiChatService.getHistory(userId, 10);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).content()).isEqualTo("Message 1");
    }
}
