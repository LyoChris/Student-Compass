package org.backendcompas.modules.budget.service;

import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.budget.dto.BudgetCategoryEntryDto;
import org.backendcompas.modules.budget.dto.BudgetPlanResponseDto;
import org.backendcompas.modules.budget.model.BudgetCategory;
import org.backendcompas.modules.budget.model.BudgetPlan;
import org.backendcompas.modules.budget.model.BudgetPlanCategoryEntry;
import org.backendcompas.modules.budget.repository.BudgetPlanRepository;
import org.backendcompas.modules.profile.dto.FixedExpenseDto;
import org.backendcompas.modules.profile.model.FixedExpense;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.backendcompas.modules.budget.model.BudgetCategory.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetPlanRepository planRepository;
    private final StudentProfileRepository profileRepository;

    @Override
    public BudgetPlanResponseDto getPlan(UUID userId) {
        BudgetPlan plan = planRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No budget plan found for user: " + userId));
        StudentProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found for user: " + userId));
        return toDto(plan, toFixedExpenseDtos(profile));
    }

    @Override
    @Transactional
    public BudgetPlanResponseDto recompute(UUID userId) {
        StudentProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("No profile found for user: " + userId));

        BigDecimal fixedTotal = profile.getFixedExpenses().stream()
                .map(FixedExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal disposable = profile.getMonthlyBudget().subtract(fixedTotal);
        if (disposable.signum() < 0) {
            disposable = BigDecimal.ZERO;
        }

        Map<BudgetCategory, Double> weights = buildWeights(profile);
        final BigDecimal finalDisposable = disposable;
        List<BudgetPlanCategoryEntry> entries = weights.entrySet().stream()
                .map(e -> new BudgetPlanCategoryEntry(
                        e.getKey(),
                        finalDisposable.multiply(BigDecimal.valueOf(e.getValue()))
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        BudgetPlan plan = planRepository.findByUserId(userId)
                .orElseGet(() -> {
                    BudgetPlan p = new BudgetPlan();
                    p.setUserId(userId);
                    return p;
                });

        plan.setMonthlyBudget(profile.getMonthlyBudget());
        plan.setFixedTotal(fixedTotal);
        plan.setDisposable(disposable);
        plan.getCategories().clear();
        plan.getCategories().addAll(entries);
        planRepository.save(plan);

        return toDto(plan, toFixedExpenseDtos(profile));
    }

    // -------------------------------------------------------------------------
    // Weight matrix (deterministic — no LLM)
    // -------------------------------------------------------------------------

    private Map<BudgetCategory, Double> buildWeights(StudentProfile profile) {
        Map<BudgetCategory, Double> w = new EnumMap<>(BudgetCategory.class);
        w.put(FOOD,      0.35);
        w.put(TRANSPORT, 0.12);
        w.put(HOUSING,   0.10);
        w.put(SUPPLIES,  0.08);
        w.put(PERSONAL,  0.10);
        w.put(LEISURE,   0.10);
        w.put(SAVINGS,   0.15);

        switch (profile.getEatingHabit()) {
            case COOKING    -> w.computeIfPresent(FOOD, (k, v) -> v * 0.85);
            case EATING_OUT,
                 DELIVERY   -> w.computeIfPresent(FOOD, (k, v) -> v * 1.25);
            default         -> { /* CANTEEN — no adjustment */ }
        }

        switch (profile.getLivingArea()) {
            case DORMITORY -> {
                w.computeIfPresent(HOUSING,   (k, v) -> v * 0.4);
                w.computeIfPresent(TRANSPORT, (k, v) -> v * 0.7);
            }
            case RENT      -> w.computeIfPresent(HOUSING,   (k, v) -> v * 1.3);
            case COMMUTER  -> w.computeIfPresent(TRANSPORT, (k, v) -> v * 1.6);
            default        -> { /* OWN_HOME — no adjustment */ }
        }

        switch (profile.getHomePackageFrequency()) {
            case WEEKLY  -> w.computeIfPresent(FOOD, (k, v) -> v * 0.8);
            case NONE    -> w.computeIfPresent(FOOD, (k, v) -> v * 1.1);
            default      -> { /* BI_WEEKLY / MONTHLY — no adjustment */ }
        }

        double total = w.values().stream().mapToDouble(Double::doubleValue).sum();
        w.replaceAll((k, v) -> v / total);
        return w;
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private List<FixedExpenseDto> toFixedExpenseDtos(StudentProfile profile) {
        return profile.getFixedExpenses().stream()
                .map(e -> new FixedExpenseDto(e.getName(), e.getAmount()))
                .toList();
    }

    private BudgetPlanResponseDto toDto(BudgetPlan plan, List<FixedExpenseDto> fixedExpenses) {
        List<BudgetCategoryEntryDto> categories = plan.getCategories().stream()
                .map(e -> new BudgetCategoryEntryDto(e.getCategory(), e.getAmount()))
                .sorted((a, b) -> a.category().compareTo(b.category()))
                .toList();

        return new BudgetPlanResponseDto(
                plan.getId(),
                plan.getUserId(),
                plan.getMonthlyBudget(),
                plan.getFixedTotal(),
                plan.getDisposable(),
                categories,
                fixedExpenses,
                plan.getUpdatedAt()
        );
    }
}
