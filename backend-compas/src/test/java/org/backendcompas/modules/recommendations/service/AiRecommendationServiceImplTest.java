package org.backendcompas.modules.recommendations.service;

import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.budget.model.BudgetCategory;
import org.backendcompas.modules.budget.model.MonthlyBudget;
import org.backendcompas.modules.budget.repository.MonthlyBudgetRepository;
import org.backendcompas.modules.profile.model.EatingHabit;
import org.backendcompas.modules.profile.model.HomePackageFrequency;
import org.backendcompas.modules.profile.model.LivingArea;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.backendcompas.modules.recommendations.dto.AiRecommendationResponseDto;
import org.backendcompas.modules.recommendations.exception.AiServiceTimeoutException;
import org.backendcompas.modules.recommendations.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceImplTest {

    @Mock
    private RestClient aiRestClient;

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private MonthlyBudgetRepository budgetRepository;

    @InjectMocks
    private AiRecommendationServiceImpl service;

    @Test
    void throwsNotFoundExceptionIfProfileMissing() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.getContextualRecommendations(userId, "FOOD", null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Student profile not found");
    }

    @Test
    void successfullyRetrievesRecommendations() {
        UUID userId = UUID.randomUUID();
        
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setLivingArea(LivingArea.DORMITORY);
        profile.setEatingHabit(EatingHabit.COOKING);
        profile.setHomePackageFrequency(HomePackageFrequency.WEEKLY);
        
        MonthlyBudget budget = new MonthlyBudget();
        BudgetCategory cat = new BudgetCategory();
        cat.setName("FOOD");
        cat.setAllocatedAmount(BigDecimal.valueOf(500));
        cat.setSpentAmount(BigDecimal.valueOf(200));
        budget.getCategories().add(cat);
        
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(budgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.of(budget));
                
        // Mocking RestClient is tricky because it's fluent. 
        // We will mock the builder and deep stub it.
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        
        when(aiRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v1/recommendations")).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.accept(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        
        AiRecommendationResponseDto mockResp = new AiRecommendationResponseDto(userId, "llm", Collections.emptyList());
        when(responseSpec.body(AiRecommendationResponseDto.class)).thenReturn(mockResp);
        
        AiRecommendationResponseDto result = service.getContextualRecommendations(userId, "FOOD", "vegan");
        
        assertThat(result.source()).isEqualTo("llm");
    }

    @Test
    void throwsAiServiceTimeoutException() {
        UUID userId = UUID.randomUUID();
        
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(budgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
                
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        
        when(aiRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.accept(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        
        when(bodySpec.retrieve()).thenThrow(new ResourceAccessException("Timeout"));
        
        assertThatThrownBy(() -> service.getContextualRecommendations(userId, "FOOD", null))
                .isInstanceOf(AiServiceTimeoutException.class);
    }
    
    @Test
    void throwsAiServiceUnavailableExceptionOnRestClientResponseException() {
        UUID userId = UUID.randomUUID();
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(budgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
                
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        
        when(aiRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.accept(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        
        when(bodySpec.retrieve()).thenThrow(new RestClientResponseException("Error", HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null, null, null));
        
        assertThatThrownBy(() -> service.getContextualRecommendations(userId, "FOOD", null))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
    
    @Test
    void throwsAiServiceUnavailableExceptionOnGenericException() {
        UUID userId = UUID.randomUUID();
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(budgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
                
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        
        when(aiRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.accept(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        
        when(bodySpec.retrieve()).thenThrow(new RuntimeException("Generic Error"));
        
        assertThatThrownBy(() -> service.getContextualRecommendations(userId, "FOOD", null))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
    
    @Test
    void throwsAiServiceUnavailableExceptionOnNullBody() {
        UUID userId = UUID.randomUUID();
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(budgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
                
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        
        when(aiRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.accept(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AiRecommendationResponseDto.class)).thenReturn(null);
        
        assertThatThrownBy(() -> service.getContextualRecommendations(userId, "FOOD", null))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void throwsAiServiceUnavailableExceptionOnRestClientException() {
        UUID userId = UUID.randomUUID();
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(budgetRepository.findByUserIdAndMonthAndYear(eq(userId), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);

        when(aiRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.accept(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);

        when(bodySpec.retrieve()).thenThrow(new RestClientException("Connection error"));

        assertThatThrownBy(() -> service.getContextualRecommendations(userId, "FOOD", null))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
}
