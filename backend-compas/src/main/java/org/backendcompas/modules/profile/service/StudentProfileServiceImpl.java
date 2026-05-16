package org.backendcompas.modules.profile.service;

import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.budget.service.BudgetService;
import org.backendcompas.modules.profile.dto.FixedExpenseDto;
import org.backendcompas.modules.profile.dto.FixedExpenseRequestDto;
import org.backendcompas.modules.profile.dto.StudentProfileRequestDto;
import org.backendcompas.modules.profile.dto.StudentProfileResponseDto;
import org.backendcompas.modules.profile.model.FixedExpense;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.backendcompas.modules.radar.repository.DormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final DormRepository dormRepository;
    private final BudgetService budgetService;

    @Override
    public StudentProfileResponseDto getProfile(UUID userId) {
        return profileRepository.findById(userId)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException(
                        "No financial profile found for user: " + userId));
    }

    @Override
    @Transactional
    public UpsertResult upsert(UUID userId, StudentProfileRequestDto dto) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }

        if (dto.getDormId() != null && !dormRepository.existsById(dto.getDormId())) {
            throw new BadRequestException("Dorm not found: " + dto.getDormId());
        }

        boolean isNew = !profileRepository.existsById(userId);

        StudentProfile profile = isNew
                ? new StudentProfile()
                : profileRepository.findById(userId).orElseThrow();

        if (isNew) {
            profile.setUserId(userId);
        }

        profile.setLivingArea(dto.getLivingArea());
        profile.setEatingHabit(dto.getEatingHabit());
        profile.setHomePackageFrequency(dto.getHomePackageFrequency());
        profile.setMonthlyBudget(dto.getMonthlyBudget());
        profile.setDormId(dto.getDormId());

        profile.getFixedExpenses().clear();
        List<FixedExpense> expenses = dto.getFixedExpenses()
                .stream()
                .map(this::toEmbeddable)
                .toList();
        profile.getFixedExpenses().addAll(expenses);

        profileRepository.save(profile);
        budgetService.recompute(userId);

        return new UpsertResult(toDto(profile), isNew);
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private FixedExpense toEmbeddable(FixedExpenseRequestDto dto) {
        return new FixedExpense(dto.getName(), dto.getAmount());
    }

    private FixedExpenseDto toExpenseDto(FixedExpense e) {
        return new FixedExpenseDto(e.getName(), e.getAmount());
    }

    private StudentProfileResponseDto toDto(StudentProfile profile) {
        List<FixedExpenseDto> expenses = profile.getFixedExpenses()
                .stream()
                .map(this::toExpenseDto)
                .toList();

        return new StudentProfileResponseDto(
                profile.getUserId(),
                profile.getLivingArea(),
                profile.getEatingHabit(),
                profile.getHomePackageFrequency(),
                profile.getMonthlyBudget(),
                expenses,
                profile.getDormId(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
