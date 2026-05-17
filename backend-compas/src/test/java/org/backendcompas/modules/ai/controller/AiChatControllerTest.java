package org.backendcompas.modules.ai.controller;

import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.ai.dto.ChatMessageDto;
import org.backendcompas.modules.ai.dto.ChatRequestDto;
import org.backendcompas.modules.ai.dto.ChatResponseDto;
import org.backendcompas.modules.ai.model.ChatRole;
import org.backendcompas.modules.ai.service.AiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AiChatControllerTest {

    private AiChatService aiChatService;
    private AiChatController controller;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        aiChatService = Mockito.mock(AiChatService.class);
        controller = new AiChatController(aiChatService);

        UUID userId = UUID.randomUUID();
        org.backendcompas.modules.account.model.User user = new org.backendcompas.modules.account.model.User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setPasswordHash("password");
        user.setRole(org.backendcompas.modules.account.model.UserRole.USER);
        user.setStatus(org.backendcompas.modules.account.model.UserStatus.ACTIVE);
        userDetails = new CustomUserDetails(user);
    }

    @Test
    void chatReturnsResponse() {
        ChatRequestDto request = new ChatRequestDto("Tell me a joke");
        ChatResponseDto response = new ChatResponseDto(UUID.randomUUID(), "Why did the chicken cross the road?");
        
        when(aiChatService.chat(eq(userDetails.getUserId()), any(ChatRequestDto.class))).thenReturn(response);
        
        ResponseEntity<ChatResponseDto> result = controller.chat(userDetails, request);
        
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().reply()).isEqualTo("Why did the chicken cross the road?");
    }

    @Test
    void getHistoryReturnsPage() {
        ChatMessageDto msg = new ChatMessageDto(UUID.randomUUID(), ChatRole.USER, "Hello", Instant.now());
        Page<ChatMessageDto> page = new PageImpl<>(List.of(msg));
        
        when(aiChatService.getHistory(userDetails.getUserId(), 20)).thenReturn(page);
        
        ResponseEntity<Page<ChatMessageDto>> result = controller.getHistory(userDetails, 20);
        
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).content()).isEqualTo("Hello");
    }
}
