package org.backendcompas.modules.recommendations.controller;

import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.UnauthorizedException;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.recommendations.dto.AiRecommendationResponseDto;
import org.backendcompas.modules.recommendations.service.AiRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RecommendationControllerTest {

    private AiRecommendationService recommendationService;
    private RecommendationController controller;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        recommendationService = Mockito.mock(AiRecommendationService.class);
        controller = new RecommendationController(recommendationService);

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
    void getRecommendationsReturnsOk() {
        AiRecommendationResponseDto responseDto = new AiRecommendationResponseDto(userDetails.getUserId(), "llm", Collections.emptyList());
        
        when(recommendationService.getContextualRecommendations(eq(userDetails.getUserId()), eq("FOOD"), any()))
                .thenReturn(responseDto);
                
        ResponseEntity<AiRecommendationResponseDto> result = controller.getRecommendations(userDetails, "FOOD", null);
        
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().source()).isEqualTo("llm");
    }

    @Test
    void getRecommendationsThrowsUnauthorizedIfUserNull() {
        assertThatThrownBy(() -> controller.getRecommendations(null, "FOOD", null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getRecommendationsRejectsUnknownCategory() {
        assertThatThrownBy(() -> controller.getRecommendations(userDetails, "INVALID_CATEGORY", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown budget category");
    }

    @Test
    void getRecommendationsRejectsBlankCategory() {
        assertThatThrownBy(() -> controller.getRecommendations(userDetails, "   ", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("category must not be blank");
    }
}
