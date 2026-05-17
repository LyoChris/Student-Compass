package org.backendcompas.modules.budget.service;

import org.backendcompas.core.exception.ForbiddenException;
import org.backendcompas.modules.budget.dto.CategoryAdjustDto;
import org.backendcompas.modules.budget.dto.ManualTransactionRequestDto;
import org.backendcompas.modules.budget.dto.MonthlyBudgetResponseDto;
import org.backendcompas.modules.budget.dto.ParsedTransactionDto;
import org.backendcompas.modules.budget.dto.SpendTodayResponseDto;
import org.backendcompas.modules.budget.model.BudgetCategory;
import org.backendcompas.modules.budget.model.MonthlyBudget;
import org.backendcompas.modules.budget.model.Transaction;
import org.backendcompas.modules.budget.repository.BudgetCategoryRepository;
import org.backendcompas.modules.budget.repository.MonthlyBudgetRepository;
import org.backendcompas.modules.budget.repository.TransactionRepository;
import org.backendcompas.modules.profile.model.EatingHabit;
import org.backendcompas.modules.profile.model.HomePackageFrequency;
import org.backendcompas.modules.profile.model.LivingArea;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private MonthlyBudgetRepository budgetRepository;

    @Mock
    private BudgetCategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private StudentProfileRepository profileRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    @Test
    void getOrCreateBudgetReturnsExisting() {
        UUID userId = UUID.randomUUID();
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(UUID.randomUUID());
        budget.setUserId(userId);
        budget.setMonth(5);
        budget.setYear(2023);
        budget.setTotalIncome(BigDecimal.valueOf(1000));
        budget.setRolloverAmount(BigDecimal.ZERO);
        
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 5, 2023)).thenReturn(Optional.of(budget));
        
        MonthlyBudgetResponseDto response = budgetService.getOrCreateBudget(userId, 5, 2023);
        
        assertThat(response.budgetId()).isEqualTo(budget.getId());
    }

    @Test
    void getOrCreateBudgetCreatesNewWithoutPrevious() {
        UUID userId = UUID.randomUUID();
        
        StudentProfile profile = new StudentProfile();
        profile.setMonthlyBudget(BigDecimal.valueOf(2000));
        profile.setLivingArea(LivingArea.DORMITORY);
        profile.setEatingHabit(EatingHabit.COOKING);
        profile.setHomePackageFrequency(HomePackageFrequency.WEEKLY);
        
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 5, 2023)).thenReturn(Optional.empty());
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 4, 2023)).thenReturn(Optional.empty());
        
        MonthlyBudgetResponseDto response = budgetService.getOrCreateBudget(userId, 5, 2023);
        
        assertThat(response.totalIncome()).isEqualTo(BigDecimal.valueOf(2000));
        assertThat(response.rolloverAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.categories()).isNotEmpty();
        
        verify(budgetRepository).save(any(MonthlyBudget.class));
    }

    @Test
    void getOrCreateBudgetCreatesNewWithPreviousRollover() {
        UUID userId = UUID.randomUUID();
        
        MonthlyBudget prevBudget = new MonthlyBudget();
        prevBudget.setId(UUID.randomUUID());
        prevBudget.setUserId(userId);
        BudgetCategory prevCat = new BudgetCategory();
        prevCat.setName("FOOD");
        prevCat.setAllocatedAmount(BigDecimal.valueOf(500));
        prevCat.setSpentAmount(BigDecimal.valueOf(300));
        prevBudget.getCategories().add(prevCat);
        
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 5, 2023)).thenReturn(Optional.empty());
        when(profileRepository.findById(userId)).thenReturn(Optional.empty()); // fallback to 1000
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 4, 2023)).thenReturn(Optional.of(prevBudget));
        
        MonthlyBudgetResponseDto response = budgetService.getOrCreateBudget(userId, 5, 2023);
        
        assertThat(response.totalIncome()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(response.rolloverAmount()).isEqualTo(BigDecimal.valueOf(200).setScale(2));
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).allocatedAmount()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(response.categories().get(0).spentAmount()).isEqualTo(BigDecimal.ZERO);
        
        verify(budgetRepository).save(any(MonthlyBudget.class));
    }

    @Test
    void adjustCategoryUpdatesExisting() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(budgetId);
        budget.setUserId(userId);
        
        BudgetCategory cat = new BudgetCategory();
        cat.setName("FOOD");
        cat.setAllocatedAmount(BigDecimal.valueOf(500));
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByBudgetIdAndNameIgnoreCase(budgetId, "FOOD")).thenReturn(Optional.of(cat));
        
        budgetService.adjustCategory(userId, budgetId, new CategoryAdjustDto("FOOD", BigDecimal.valueOf(600)));
        
        assertThat(cat.getAllocatedAmount()).isEqualTo(BigDecimal.valueOf(600));
        verify(categoryRepository).save(cat);
    }

    @Test
    void adjustCategoryCreatesNew() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(budgetId);
        budget.setUserId(userId);
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByBudgetIdAndNameIgnoreCase(budgetId, "NEW")).thenReturn(Optional.empty());
        
        budgetService.adjustCategory(userId, budgetId, new CategoryAdjustDto("NEW", BigDecimal.valueOf(100)));
        
        verify(categoryRepository).save(any(BudgetCategory.class));
    }

    @Test
    void adjustCategoryThrowsForbiddenForWrongUser() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        CategoryAdjustDto dto = new CategoryAdjustDto("FOOD", BigDecimal.TEN);
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(budgetId);
        budget.setUserId(UUID.randomUUID()); // different user
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        
        assertThatThrownBy(() -> budgetService.adjustCategory(userId, budgetId, dto))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteCategoryRemovesCategory() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(budgetId);
        budget.setUserId(userId);
        
        BudgetCategory cat = new BudgetCategory();
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByBudgetIdAndNameIgnoreCase(budgetId, "FOOD")).thenReturn(Optional.of(cat));
        
        budgetService.deleteCategory(userId, budgetId, "FOOD");
        
        verify(categoryRepository).delete(cat);
    }

    @Test
    void logManualTransactionAddsTransactionAndUpdatesCategory() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(budgetId);
        budget.setUserId(userId);
        
        BudgetCategory cat = new BudgetCategory();
        cat.setName("FOOD");
        cat.setSpentAmount(BigDecimal.valueOf(50));
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByBudgetIdAndNameIgnoreCase(budgetId, "FOOD")).thenReturn(Optional.of(cat));
        
        budgetService.logManualTransaction(userId, new ManualTransactionRequestDto(budgetId, "FOOD", BigDecimal.valueOf(20), "Lunch"));
        
        assertThat(cat.getSpentAmount()).isEqualTo(BigDecimal.valueOf(70));
        verify(transactionRepository).save(any(Transaction.class));
        verify(categoryRepository).save(cat);
    }

    @Test
    void parseBankStatementProcessesCSV() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        MonthlyBudget budget = new MonthlyBudget();
        budget.setId(budgetId);
        budget.setUserId(userId);
        
        BudgetCategory cat = new BudgetCategory();
        cat.setName("FOOD");
        cat.setSpentAmount(BigDecimal.ZERO);
        
        String csvContent = "Data,Detalii,Suma\n01-01-2023,Kaufland,-50.00\n01-01-2023,Salary,1000\n02-01-2023,Unknown,-10.00";
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", csvContent.getBytes());
        
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        // Only return category for FOOD (kaufland matches FOOD keyword)
        when(categoryRepository.findByBudgetIdAndNameIgnoreCase(budgetId, "FOOD")).thenReturn(Optional.of(cat));
        when(categoryRepository.findByBudgetIdAndNameIgnoreCase(budgetId, "PERSONAL")).thenReturn(Optional.empty()); // Unknown defaults to PERSONAL
        
        List<ParsedTransactionDto> results = budgetService.parseBankStatement(userId, budgetId, file);
        
        assertThat(results).hasSize(3);
        assertThat(results.get(0).detectedCategory()).isEqualTo("FOOD");
        assertThat(results.get(0).amount()).isEqualTo(BigDecimal.valueOf(50.0).setScale(2));
        assertThat(results.get(0).persisted()).isTrue();
        
        assertThat(results.get(1).detectedCategory()).isEqualTo("PERSONAL");
        assertThat(results.get(1).persisted()).isFalse();
        
        assertThat(results.get(2).detectedCategory()).isEqualTo("PERSONAL");
        assertThat(results.get(2).persisted()).isFalse();
        
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void getSpendTodayReturnsAggregatedData() {
        UUID userId = UUID.randomUUID();
        
        BudgetCategory foodCat = new BudgetCategory();
        foodCat.setName("FOOD");
        
        Transaction tx1 = new Transaction();
        tx1.setCategory(foodCat);
        tx1.setAmount(BigDecimal.valueOf(15));
        tx1.setDescription("Lunch");
        tx1.setTransactionDate(LocalDateTime.now());
        
        Transaction tx2 = new Transaction();
        tx2.setCategory(foodCat);
        tx2.setAmount(BigDecimal.valueOf(25));
        tx2.setDescription("Dinner");
        tx2.setTransactionDate(LocalDateTime.now());
        
        when(transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(tx1, tx2));
                
        SpendTodayResponseDto response = budgetService.getSpendToday(userId);
        
        assertThat(response.totalToday()).isEqualTo(BigDecimal.valueOf(40));
        assertThat(response.byCategory()).hasSize(1);
        assertThat(response.byCategory().get(0).categoryName()).isEqualTo("FOOD");
        assertThat(response.byCategory().get(0).amount()).isEqualTo(BigDecimal.valueOf(40));
        assertThat(response.transactions()).hasSize(2);
    }
}
