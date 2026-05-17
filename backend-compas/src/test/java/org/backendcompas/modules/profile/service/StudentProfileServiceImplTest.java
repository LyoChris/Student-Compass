package org.backendcompas.modules.profile.service;

import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.profile.dto.FixedExpenseRequestDto;
import org.backendcompas.modules.profile.dto.StudentProfileRequestDto;
import org.backendcompas.modules.profile.dto.StudentProfileResponseDto;
import org.backendcompas.modules.profile.model.EatingHabit;
import org.backendcompas.modules.profile.model.HomePackageFrequency;
import org.backendcompas.modules.profile.model.LivingArea;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.backendcompas.modules.radar.repository.DormRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceImplTest {

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DormRepository dormRepository;

    @InjectMocks
    private StudentProfileServiceImpl studentProfileService;

    @Test
    void getProfileReturnsProfileWhenExists() {
        UUID userId = UUID.randomUUID();
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setMonthlyBudget(BigDecimal.valueOf(1000));
        
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        
        StudentProfileResponseDto result = studentProfileService.getProfile(userId);
        
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.monthlyBudget()).isEqualTo(BigDecimal.valueOf(1000));
    }

    @Test
    void getProfileThrowsNotFoundWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> studentProfileService.getProfile(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No financial profile found");
    }

    @Test
    void upsertThrowsNotFoundWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        StudentProfileRequestDto dto = buildRequestDto();
        
        when(userRepository.existsById(userId)).thenReturn(false);
        
        assertThatThrownBy(() -> studentProfileService.upsert(userId, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void upsertThrowsBadRequestWhenDormMissing() {
        UUID userId = UUID.randomUUID();
        StudentProfileRequestDto dto = buildRequestDto();
        
        when(userRepository.existsById(userId)).thenReturn(true);
        when(dormRepository.existsById(dto.getDormId())).thenReturn(false);
        
        assertThatThrownBy(() -> studentProfileService.upsert(userId, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Dorm not found");
    }

    @Test
    void upsertCreatesNewProfile() {
        UUID userId = UUID.randomUUID();
        StudentProfileRequestDto dto = buildRequestDto();
        
        when(userRepository.existsById(userId)).thenReturn(true);
        when(dormRepository.existsById(dto.getDormId())).thenReturn(true);
        when(profileRepository.existsById(userId)).thenReturn(false);
        
        UpsertResult result = studentProfileService.upsert(userId, dto);
        
        assertThat(result.wasCreated()).isTrue();
        assertThat(result.profile().livingArea()).isEqualTo(LivingArea.DORMITORY);
        assertThat(result.profile().monthlyBudget()).isEqualTo(BigDecimal.valueOf(1500));
        assertThat(result.profile().fixedExpenses()).hasSize(1);
        
        verify(profileRepository).save(any(StudentProfile.class));
    }

    @Test
    void upsertUpdatesExistingProfile() {
        UUID userId = UUID.randomUUID();
        StudentProfileRequestDto dto = buildRequestDto();
        
        StudentProfile existingProfile = new StudentProfile();
        existingProfile.setUserId(userId);
        existingProfile.setMonthlyBudget(BigDecimal.valueOf(500));
        
        when(userRepository.existsById(userId)).thenReturn(true);
        when(dormRepository.existsById(dto.getDormId())).thenReturn(true);
        when(profileRepository.existsById(userId)).thenReturn(true);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(existingProfile));
        
        UpsertResult result = studentProfileService.upsert(userId, dto);
        
        assertThat(result.wasCreated()).isFalse();
        assertThat(result.profile().monthlyBudget()).isEqualTo(BigDecimal.valueOf(1500));
        
        verify(profileRepository).save(existingProfile);
    }

    private StudentProfileRequestDto buildRequestDto() {
        StudentProfileRequestDto dto = new StudentProfileRequestDto();
        dto.setLivingArea(LivingArea.DORMITORY);
        dto.setEatingHabit(EatingHabit.COOKING);
        dto.setHomePackageFrequency(HomePackageFrequency.WEEKLY);
        dto.setMonthlyBudget(BigDecimal.valueOf(1500));
        dto.setDormId(UUID.randomUUID());
        dto.setFixedExpenses(List.of(new FixedExpenseRequestDto("Gym", BigDecimal.valueOf(100))));
        return dto;
    }
}
