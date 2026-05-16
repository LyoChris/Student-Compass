package org.backendcompas.modules.budget.service;

import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.BadRequestException;
import org.backendcompas.core.exception.ForbiddenException;
import org.backendcompas.core.exception.NotFoundException;
import org.backendcompas.modules.budget.dto.BudgetCategoryResponseDto;
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
import org.backendcompas.modules.profile.model.FixedExpense;
import org.backendcompas.modules.profile.model.StudentProfile;
import org.backendcompas.modules.profile.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    // -------------------------------------------------------------------------
    // Keyword → category routing table (checked in declaration order; first match wins)
    // -------------------------------------------------------------------------
    private static final Map<String, String> KEYWORD_MAP = new LinkedHashMap<>();

    static {
        // Food & drink
        KEYWORD_MAP.put("kaufland",    "FOOD");
        KEYWORD_MAP.put("lidl",        "FOOD");
        KEYWORD_MAP.put("auchan",      "FOOD");
        KEYWORD_MAP.put("carrefour",   "FOOD");
        KEYWORD_MAP.put("mega image",  "FOOD");
        KEYWORD_MAP.put("profi",       "FOOD");
        KEYWORD_MAP.put("penny",       "FOOD");
        KEYWORD_MAP.put("glovo",       "FOOD");
        KEYWORD_MAP.put("bolt food",   "FOOD");
        KEYWORD_MAP.put("tazz",        "FOOD");
        KEYWORD_MAP.put("restaurant",  "FOOD");
        KEYWORD_MAP.put("cafenea",     "FOOD");
        KEYWORD_MAP.put("bistro",      "FOOD");
        KEYWORD_MAP.put("mcdonald",    "FOOD");
        KEYWORD_MAP.put("kfc",         "FOOD");
        // Transport (checked before "bolt" to catch "bolt food" first)
        KEYWORD_MAP.put("uber",        "TRANSPORT");
        KEYWORD_MAP.put("bolt",        "TRANSPORT");
        KEYWORD_MAP.put("stb ",        "TRANSPORT");
        KEYWORD_MAP.put("ratb",        "TRANSPORT");
        KEYWORD_MAP.put("metrorex",    "TRANSPORT");
        KEYWORD_MAP.put("petrom",      "TRANSPORT");
        KEYWORD_MAP.put("omv",         "TRANSPORT");
        KEYWORD_MAP.put("mol ",        "TRANSPORT");
        KEYWORD_MAP.put("benzinarie",  "TRANSPORT");
        KEYWORD_MAP.put("parking",     "TRANSPORT");
        // Housing & utilities
        KEYWORD_MAP.put("chirie",      "HOUSING");
        KEYWORD_MAP.put("rent",        "HOUSING");
        KEYWORD_MAP.put("utilit",      "HOUSING");
        KEYWORD_MAP.put("enel",        "HOUSING");
        KEYWORD_MAP.put("romgaz",      "HOUSING");
        KEYWORD_MAP.put("rcs",         "HOUSING");
        KEYWORD_MAP.put("rds",         "HOUSING");
        KEYWORD_MAP.put("digi",        "HOUSING");
        KEYWORD_MAP.put("orange",      "HOUSING");
        KEYWORD_MAP.put("vodafone",    "HOUSING");
        // Personal care
        KEYWORD_MAP.put("farmaci",     "PERSONAL");
        KEYWORD_MAP.put("sensiblu",    "PERSONAL");
        KEYWORD_MAP.put("catena",      "PERSONAL");
        KEYWORD_MAP.put("dm ",         "PERSONAL");
        KEYWORD_MAP.put("cosmet",      "PERSONAL");
        KEYWORD_MAP.put("salon",       "PERSONAL");
        KEYWORD_MAP.put("coafura",     "PERSONAL");
        KEYWORD_MAP.put("frizerie",    "PERSONAL");
        // Leisure & entertainment
        KEYWORD_MAP.put("cinema",      "LEISURE");
        KEYWORD_MAP.put("netflix",     "LEISURE");
        KEYWORD_MAP.put("spotify",     "LEISURE");
        KEYWORD_MAP.put("hbo",         "LEISURE");
        KEYWORD_MAP.put("steam",       "LEISURE");
        KEYWORD_MAP.put("club",        "LEISURE");
        KEYWORD_MAP.put("concert",     "LEISURE");
        KEYWORD_MAP.put("bilet",       "LEISURE");
        // Supplies
        KEYWORD_MAP.put("librarie",    "SUPPLIES");
        KEYWORD_MAP.put("papetarie",   "SUPPLIES");
        KEYWORD_MAP.put("printing",    "SUPPLIES");
        KEYWORD_MAP.put("xerox",       "SUPPLIES");
        KEYWORD_MAP.put("copiat",      "SUPPLIES");
    }

    // Default weight matrix (sums to 1.0)
    private static final Map<String, Double> DEFAULT_WEIGHTS = new LinkedHashMap<>();

    static {
        DEFAULT_WEIGHTS.put("FOOD",      0.30);
        DEFAULT_WEIGHTS.put("TRANSPORT", 0.12);
        DEFAULT_WEIGHTS.put("HOUSING",   0.15);
        DEFAULT_WEIGHTS.put("SUPPLIES",  0.08);
        DEFAULT_WEIGHTS.put("PERSONAL",  0.10);
        DEFAULT_WEIGHTS.put("LEISURE",   0.10);
        DEFAULT_WEIGHTS.put("SAVINGS",   0.15);
    }

    private final MonthlyBudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final StudentProfileRepository profileRepository;

    // =========================================================================
    // getOrCreateBudget
    // =========================================================================

    @Override
    @Transactional
    public MonthlyBudgetResponseDto getOrCreateBudget(UUID userId, int month, int year) {
        Optional<MonthlyBudget> existing = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        MonthlyBudget budget = new MonthlyBudget();
        budget.setUserId(userId);
        budget.setMonth(month);
        budget.setYear(year);

        // Determine income: from request, profile, or fallback
        BigDecimal income = profileRepository.findById(userId)
                .map(StudentProfile::getMonthlyBudget)
                .orElse(BigDecimal.valueOf(1000));
        budget.setTotalIncome(income);

        // Check previous month for rollover + category template
        YearMonth prevYM = YearMonth.of(year, month).minusMonths(1);
        Optional<MonthlyBudget> prev = budgetRepository
                .findByUserIdAndMonthAndYear(userId, prevYM.getMonthValue(), prevYM.getYear());

        if (prev.isPresent()) {
            MonthlyBudget prevBudget = prev.get();

            // Rollover = sum of positive (allocated - spent) across all previous categories
            BigDecimal rollover = prevBudget.getCategories().stream()
                    .map(c -> c.getAllocatedAmount().subtract(c.getSpentAmount()))
                    .filter(diff -> diff.signum() > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            budget.setRolloverAmount(rollover);

            // Copy categories from previous month (zero out spent amounts)
            for (BudgetCategory prevCat : prevBudget.getCategories()) {
                BudgetCategory cat = new BudgetCategory();
                cat.setBudget(budget);
                cat.setName(prevCat.getName());
                cat.setAllocatedAmount(prevCat.getAllocatedAmount());
                cat.setSpentAmount(BigDecimal.ZERO);
                budget.getCategories().add(cat);
            }
        } else {
            budget.setRolloverAmount(BigDecimal.ZERO);
            budget.getCategories().addAll(buildDefaultCategories(budget, income));
        }

        budgetRepository.save(budget);
        return toDto(budget);
    }

    // =========================================================================
    // adjustCategory
    // =========================================================================

    @Override
    @Transactional
    public void adjustCategory(UUID userId, UUID budgetId, CategoryAdjustDto dto) {
        MonthlyBudget budget = findBudget(budgetId);
        assertOwner(budget, userId);

        Optional<BudgetCategory> existing = categoryRepository
                .findByBudgetIdAndNameIgnoreCase(budgetId, dto.categoryName());

        if (existing.isPresent()) {
            existing.get().setAllocatedAmount(dto.newAllocation());
            categoryRepository.save(existing.get());
        } else {
            BudgetCategory cat = new BudgetCategory();
            cat.setBudget(budget);
            cat.setName(dto.categoryName().toUpperCase());
            cat.setAllocatedAmount(dto.newAllocation());
            cat.setSpentAmount(BigDecimal.ZERO);
            categoryRepository.save(cat);
        }
    }

    // =========================================================================
    // deleteCategory
    // =========================================================================

    @Override
    @Transactional
    public void deleteCategory(UUID userId, UUID budgetId, String categoryName) {
        MonthlyBudget budget = findBudget(budgetId);
        assertOwner(budget, userId);

        BudgetCategory cat = categoryRepository
                .findByBudgetIdAndNameIgnoreCase(budgetId, categoryName)
                .orElseThrow(() -> new NotFoundException(
                        "Category '" + categoryName + "' not found in budget " + budgetId));

        categoryRepository.delete(cat);
    }

    // =========================================================================
    // logManualTransaction
    // =========================================================================

    @Override
    @Transactional
    public void logManualTransaction(UUID userId, ManualTransactionRequestDto dto) {
        MonthlyBudget budget = findBudget(dto.budgetId());
        assertOwner(budget, userId);

        BudgetCategory cat = categoryRepository
                .findByBudgetIdAndNameIgnoreCase(dto.budgetId(), dto.categoryName())
                .orElseThrow(() -> new NotFoundException(
                        "Category '" + dto.categoryName() + "' not found in budget " + dto.budgetId()));

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setCategory(cat);
        tx.setAmount(dto.amount());
        tx.setDescription(dto.description());
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);

        cat.setSpentAmount(cat.getSpentAmount().add(dto.amount()));
        categoryRepository.save(cat);
    }

    // =========================================================================
    // parseBankStatement
    // =========================================================================

    @Override
    @Transactional
    public List<ParsedTransactionDto> parseBankStatement(UUID userId, UUID budgetId, MultipartFile file) {
        MonthlyBudget budget = findBudget(budgetId);
        assertOwner(budget, userId);

        List<ParsedTransactionDto> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return results;
            }

            String[] headers = parseCsvLine(headerLine);
            int descIdx = detectColumnIndex(headers, "description", "descriere", "merchant", "beneficiar", "narration", "details");
            int amountIdx = detectColumnIndex(headers, "amount", "money out", "debit", "suma", "withdrawal", "out");

            // Fallback to column 1 (index 1) if no description column found
            if (descIdx < 0) {
                descIdx = Math.min(1, headers.length - 1);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isBlank()) {
                    continue;
                }

                String[] cols = parseCsvLine(line);
                if (cols.length <= descIdx) {
                    continue;
                }

                String desc = cols[descIdx].strip();
                if (desc.isBlank()) {
                    continue;
                }

                BigDecimal amount = extractAmount(cols, amountIdx);
                if (amount == null || amount.signum() <= 0) {
                    continue; // skip income / unparseable rows
                }

                String detectedCategory = detectCategory(desc);

                Optional<BudgetCategory> catOpt = categoryRepository
                        .findByBudgetIdAndNameIgnoreCase(budgetId, detectedCategory);

                boolean persisted = false;
                if (catOpt.isPresent()) {
                    BudgetCategory cat = catOpt.get();

                    Transaction tx = new Transaction();
                    tx.setUserId(userId);
                    tx.setCategory(cat);
                    tx.setAmount(amount);
                    tx.setDescription(desc);
                    tx.setTransactionDate(LocalDateTime.now());
                    transactionRepository.save(tx);

                    cat.setSpentAmount(cat.getSpentAmount().add(amount));
                    categoryRepository.save(cat);
                    persisted = true;
                }

                results.add(new ParsedTransactionDto(desc, amount, detectedCategory, persisted));
            }

        } catch (IOException e) {
            throw new BadRequestException("Failed to read bank statement: " + e.getMessage());
        }

        return results;
    }

    // =========================================================================
    // getSpendToday
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SpendTodayResponseDto getSpendToday(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to   = today.plusDays(1).atStartOfDay();

        List<Transaction> txs = transactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, from, to);

        BigDecimal total = txs.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // per-category aggregation
        Map<String, BigDecimal> catMap = new LinkedHashMap<>();
        for (Transaction tx : txs) {
            String name = tx.getCategory().getName();
            catMap.merge(name, tx.getAmount(), BigDecimal::add);
        }

        List<SpendTodayResponseDto.CategorySpendDto> byCategory = catMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new SpendTodayResponseDto.CategorySpendDto(e.getKey(), e.getValue()))
                .toList();

        List<SpendTodayResponseDto.TransactionItemDto> items = txs.stream()
                .map(tx -> new SpendTodayResponseDto.TransactionItemDto(
                        tx.getCategory().getName(),
                        tx.getAmount(),
                        tx.getDescription(),
                        tx.getTransactionDate().toString()
                ))
                .toList();

        return new SpendTodayResponseDto(today, total, byCategory, items);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private MonthlyBudget findBudget(UUID budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new NotFoundException("Budget not found: " + budgetId));
    }

    private void assertOwner(MonthlyBudget budget, UUID userId) {
        if (!budget.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify this budget");
        }
    }

    private MonthlyBudgetResponseDto toDto(MonthlyBudget budget) {
        List<BudgetCategoryResponseDto> cats = budget.getCategories().stream()
                .map(c -> new BudgetCategoryResponseDto(
                        c.getId(),
                        c.getName(),
                        c.getAllocatedAmount(),
                        c.getSpentAmount(),
                        c.getAllocatedAmount().subtract(c.getSpentAmount()).max(BigDecimal.ZERO)
                ))
                .sorted(Comparator.comparing(BudgetCategoryResponseDto::name))
                .toList();

        BigDecimal totalAllocated = cats.stream()
                .map(BudgetCategoryResponseDto::allocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = cats.stream()
                .map(BudgetCategoryResponseDto::spentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = totalAllocated
                .subtract(totalSpent)
                .add(budget.getRolloverAmount())
                .max(BigDecimal.ZERO);

        BigDecimal fixedTotal = getFixedTotal(budget.getUserId());
        BigDecimal s2s = calcSafeToSpend(
                budget.getTotalIncome(),
                fixedTotal,
                totalSpent,
                budget.getMonth(),
                budget.getYear()
        );

        return new MonthlyBudgetResponseDto(
                budget.getId(),
                budget.getUserId(),
                budget.getMonth(),
                budget.getYear(),
                budget.getTotalIncome(),
                budget.getRolloverAmount(),
                totalAllocated,
                totalSpent,
                totalRemaining,
                s2s,
                cats,
                budget.getUpdatedAt()
        );
    }

    private BigDecimal getFixedTotal(UUID userId) {
        return profileRepository.findById(userId)
                .map(StudentProfile::getFixedExpenses)
                .stream()
                .flatMap(List::stream)
                .map(FixedExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcSafeToSpend(
            BigDecimal monthlyBudget,
            BigDecimal fixedTotal,
            BigDecimal totalSpent,
            int month,
            int year
    ) {
        BigDecimal remainingDisposable = monthlyBudget
                .subtract(fixedTotal)
                .subtract(totalSpent);

        if (remainingDisposable.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth();

        int remainingDays;
        YearMonth currentYM = YearMonth.from(today);
        if (ym.equals(currentYM)) {
            remainingDays = daysInMonth - today.getDayOfMonth();
            if (remainingDays == 0) {
                remainingDays = 1;
            }
        } else if (ym.isAfter(currentYM)) {
            remainingDays = daysInMonth;
        } else {
            remainingDays = 1; // past month — budget is effectively closed
        }

        return remainingDisposable.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP);
    }

    private List<BudgetCategory> buildDefaultCategories(MonthlyBudget budget, BigDecimal income) {
        Map<String, Double> weights = buildWeightsFromProfile(budget.getUserId());

        return weights.entrySet().stream()
                .map(e -> {
                    BudgetCategory cat = new BudgetCategory();
                    cat.setBudget(budget);
                    cat.setName(e.getKey());
                    cat.setAllocatedAmount(
                            income.multiply(BigDecimal.valueOf(e.getValue()))
                                    .setScale(2, RoundingMode.HALF_UP)
                    );
                    cat.setSpentAmount(BigDecimal.ZERO);
                    return cat;
                })
                .toList();
    }

    private Map<String, Double> buildWeightsFromProfile(UUID userId) {
        StudentProfile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return DEFAULT_WEIGHTS;
        }

        Map<String, Double> w = new LinkedHashMap<>(DEFAULT_WEIGHTS);

        switch (profile.getEatingHabit()) {
            case COOKING     -> w.computeIfPresent("FOOD", (k, v) -> v * 0.85);
            case EATING_OUT,
                 DELIVERY    -> w.computeIfPresent("FOOD", (k, v) -> v * 1.25);
            default          -> { /* CANTEEN — no adjustment */ }
        }

        switch (profile.getLivingArea()) {
            case DORMITORY -> {
                w.computeIfPresent("HOUSING",   (k, v) -> v * 0.40);
                w.computeIfPresent("TRANSPORT", (k, v) -> v * 0.70);
            }
            case RENT      -> w.computeIfPresent("HOUSING",   (k, v) -> v * 1.30);
            case COMMUTER  -> w.computeIfPresent("TRANSPORT", (k, v) -> v * 1.60);
            default        -> { /* OWN_HOME — no adjustment */ }
        }

        switch (profile.getHomePackageFrequency()) {
            case WEEKLY -> w.computeIfPresent("FOOD", (k, v) -> v * 0.80);
            case NONE   -> w.computeIfPresent("FOOD", (k, v) -> v * 1.10);
            default     -> { /* BI_WEEKLY / MONTHLY */ }
        }

        double total = w.values().stream().mapToDouble(Double::doubleValue).sum();
        w.replaceAll((k, v) -> Math.round((v / total) * 10_000d) / 10_000d);
        return w;
    }

    // -------------------------------------------------------------------------
    // CSV helpers
    // -------------------------------------------------------------------------

    private int detectColumnIndex(String[] headers, String... candidates) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].toLowerCase().strip().replace("\"", "");
            for (String candidate : candidates) {
                if (h.contains(candidate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private BigDecimal extractAmount(String[] cols, int amountIdx) {
        if (amountIdx < 0 || amountIdx >= cols.length) {
            return null;
        }
        String raw = cols[amountIdx].strip()
                .replace("\"", "")
                .replace(" ", "")
                .replace(",", ".");
        if (raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw).abs();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Minimal CSV parser that respects double-quoted fields containing commas. */
    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString());
        return tokens.toArray(new String[0]);
    }

    private String detectCategory(String description) {
        String lower = description.toLowerCase();
        for (Map.Entry<String, String> entry : KEYWORD_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "PERSONAL"; // default fallback
    }
}
