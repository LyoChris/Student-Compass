package org.backendcompas.modules.budget.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.budget.dto.CategoryAdjustDto;
import org.backendcompas.modules.budget.dto.ManualTransactionRequestDto;
import org.backendcompas.modules.budget.dto.MonthlyBudgetResponseDto;
import org.backendcompas.modules.budget.dto.ParsedTransactionDto;
import org.backendcompas.modules.budget.dto.SpendTodayResponseDto;
import org.backendcompas.modules.budget.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(
        name = "Budget",
        description = """
                Dynamic month-by-month personal finance planner for students.

                ## Budget lifecycle
                1. Call `GET /current` for any `month` + `year`. If no budget exists for that period
                   it is automatically created — either by copying the previous month's categories
                   (with rollover surplus) or via the default weight-matrix template.
                2. Adjust allocations or add custom categories with `PUT /{budgetId}/categories`.
                3. Log day-to-day spending via `POST /transactions` or bulk-import a Revolut / ING
                   CSV with `POST /{budgetId}/upload-statement`.
                4. Delete categories you no longer need with `DELETE /{budgetId}/categories/{name}`.

                ## Safe-to-Spend (S2S)
                Every `GET /current` response includes `safeToSpendPerDay`:
                ```
                S2S = totalRemaining / remainingDaysInMonth
                ```
                - **Current month** — remaining days = max(1, daysInMonth − today + 1)
                - **Future month** — remaining days = full month length
                - **Past month**   — remaining days = 1 (budget is closed)

                ## Rollover
                When a new month budget is created and a previous month exists, any positive
                `allocated − spent` surplus per category is summed and added to the new month's
                `rolloverAmount`, which is included in `totalRemaining` and therefore in S2S.

                ## Ownership
                Every write endpoint verifies that the authenticated user owns the target budget.
                A mismatch returns `403 Forbidden`.
                """
)
@RestController
@RequestMapping(value = "/api/v1/budgets", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
@Validated
public class BudgetController {

    private final BudgetService budgetService;

    // =========================================================================
    // GET /current
    // =========================================================================

    @Operation(
            summary = "Get or create the monthly budget",
            description = """
                    Returns the budget for the specified `month` / `year`, creating it on first access.

                    **Creation strategy (first access only):**
                    - If a budget exists for the previous calendar month it is used as the template:
                      categories and allocations are copied, spent amounts are reset to 0, and any
                      surplus (`allocated − spent > 0`) is summed into `rolloverAmount`.
                    - If no previous month exists, the system generates a default template from the
                      student's profile weight matrix (COOKING / DORMITORY / etc. multipliers).
                      Falls back to standard weights when no profile is on file.

                    **Idempotent** — subsequent calls for the same month return the stored data.

                    Month and year default to the current calendar month when omitted.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Budget found or successfully created.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MonthlyBudgetResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "May 2026 — first access, previous month copied",
                                            value = """
                                                    {
                                                      "budgetId": "b1e2f3a4-5678-90ab-cdef-000000000001",
                                                      "userId":   "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "month": 5, "year": 2026,
                                                      "totalIncome":    1500.00,
                                                      "rolloverAmount":   85.50,
                                                      "totalAllocated": 1500.00,
                                                      "totalSpent":        0.00,
                                                      "totalRemaining": 1585.50,
                                                      "safeToSpendPerDay": 52.85,
                                                      "categories": [
                                                        { "id": "c1a2b3d4-0001-0000-0000-000000000001", "name": "FOOD",      "allocatedAmount": 450.00, "spentAmount": 0.00, "remaining": 450.00 },
                                                        { "id": "c1a2b3d4-0002-0000-0000-000000000001", "name": "HOUSING",   "allocatedAmount": 225.00, "spentAmount": 0.00, "remaining": 225.00 },
                                                        { "id": "c1a2b3d4-0003-0000-0000-000000000001", "name": "LEISURE",   "allocatedAmount": 150.00, "spentAmount": 0.00, "remaining": 150.00 },
                                                        { "id": "c1a2b3d4-0004-0000-0000-000000000001", "name": "PERSONAL",  "allocatedAmount": 150.00, "spentAmount": 0.00, "remaining": 150.00 },
                                                        { "id": "c1a2b3d4-0005-0000-0000-000000000001", "name": "SAVINGS",   "allocatedAmount": 225.00, "spentAmount": 0.00, "remaining": 225.00 },
                                                        { "id": "c1a2b3d4-0006-0000-0000-000000000001", "name": "SUPPLIES",  "allocatedAmount": 120.00, "spentAmount": 0.00, "remaining": 120.00 },
                                                        { "id": "c1a2b3d4-0007-0000-0000-000000000001", "name": "TRANSPORT", "allocatedAmount": 180.00, "spentAmount": 0.00, "remaining": 180.00 }
                                                      ],
                                                      "updatedAt": "2026-05-01T08:00:00Z"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "May 2026 — mid-month with spending",
                                            value = """
                                                    {
                                                      "budgetId": "b1e2f3a4-5678-90ab-cdef-000000000001",
                                                      "userId":   "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "month": 5, "year": 2026,
                                                      "totalIncome":    1500.00,
                                                      "rolloverAmount":   85.50,
                                                      "totalAllocated": 1500.00,
                                                      "totalSpent":      320.40,
                                                      "totalRemaining": 1265.10,
                                                      "safeToSpendPerDay": 42.17,
                                                      "categories": [
                                                        { "id": "c1a2b3d4-0001-0000-0000-000000000001", "name": "FOOD",      "allocatedAmount": 450.00, "spentAmount": 210.50, "remaining": 239.50 },
                                                        { "id": "c1a2b3d4-0007-0000-0000-000000000001", "name": "TRANSPORT", "allocatedAmount": 180.00, "spentAmount": 109.90, "remaining":  70.10 }
                                                      ],
                                                      "updatedAt": "2026-05-16T14:30:00Z"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No valid Bearer token provided.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":401,"error":"Unauthorized","message":"Full authentication is required","path":"/api/v1/budgets/current","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "400", description = "month or year parameter is out of range.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":400,"error":"Bad Request","message":"month must be between 1 and 12","path":"/api/v1/budgets/current","timestamp":"2026-05-16T12:00:00Z"}
                                    """)))
    })
    @GetMapping("/current")
    public ResponseEntity<MonthlyBudgetResponseDto> getCurrentBudget(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(
                    name = "month",
                    in = ParameterIn.QUERY,
                    description = "Calendar month (1–12). Defaults to the current month when omitted.",
                    example = "5",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "12")
            )
            @RequestParam(required = false) Integer month,

            @Parameter(
                    name = "year",
                    in = ParameterIn.QUERY,
                    description = "Four-digit calendar year (≥ 2020). Defaults to the current year when omitted.",
                    example = "2026",
                    schema = @Schema(type = "integer", minimum = "2020")
            )
            @RequestParam(required = false) Integer year
    ) {
        LocalDate today = LocalDate.now();
        int m = month != null ? month : today.getMonthValue();
        int y = year  != null ? year  : today.getYear();

        if (m < 1 || m > 12) {
            throw new org.backendcompas.core.exception.BadRequestException("month must be between 1 and 12");
        }
        if (y < 2020) {
            throw new org.backendcompas.core.exception.BadRequestException("year must be 2020 or later");
        }

        return ResponseEntity.ok(budgetService.getOrCreateBudget(userDetails.getUserId(), m, y));
    }

    // =========================================================================
    // GET /spend-today
    // =========================================================================

    @Operation(
            summary = "How much have I spent today?",
            description = """
                    Returns a real-time summary of everything the authenticated student has spent
                    during the current calendar day (from midnight to now).

                    Includes:
                    - `totalToday` — grand total across all categories
                    - `byCategory` — per-category breakdown, sorted by amount descending
                    - `transactions` — individual entries, newest first

                    Transactions from all budgets belonging to the user are included — the
                    response is not scoped to a specific month's budget.

                    Returns an empty summary (totalToday = 0.00, empty lists) when no
                    transactions have been logged yet today.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Today's spend summary returned successfully.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SpendTodayResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Mid-day with two categories",
                                            value = """
                                                    {
                                                      "date": "2026-05-17",
                                                      "totalToday": 143.80,
                                                      "byCategory": [
                                                        { "categoryName": "FOOD",      "amount": 94.30 },
                                                        { "categoryName": "TRANSPORT", "amount": 49.50 }
                                                      ],
                                                      "transactions": [
                                                        { "categoryName": "TRANSPORT", "amount": 49.50, "description": "Uber to faculty",          "transactionDate": "2026-05-17T11:02:14" },
                                                        { "categoryName": "FOOD",      "amount": 94.30, "description": "Kaufland — weekly groceries","transactionDate": "2026-05-17T09:14:32" }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Nothing spent yet today",
                                            value = """
                                                    {
                                                      "date": "2026-05-17",
                                                      "totalToday": 0.00,
                                                      "byCategory": [],
                                                      "transactions": []
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No valid Bearer token provided.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/spend-today")
    public ResponseEntity<SpendTodayResponseDto> getSpendToday(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(budgetService.getSpendToday(userDetails.getUserId()));
    }

    // =========================================================================
    // PUT /{budgetId}/categories
    // =========================================================================

    @Operation(
            summary = "Add or update a budget category",
            description = """
                    **Upsert semantics:**
                    - If a category with `categoryName` already exists inside the budget, its
                      `allocatedAmount` is updated in-place. `spentAmount` is not touched.
                    - If the category does not exist, a new row is created with `spentAmount = 0`.

                    Category names are matched **case-insensitively**.
                    Custom names are fully supported (e.g. `GYM`, `HOBBY`, `SUBSCRIPTIONS`).

                    **Ownership required:** the authenticated user must own this budget.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category created or updated successfully. No body returned.", content = @Content),
            @ApiResponse(responseCode = "400", description = "Request body fails validation (blank name, negative allocation, etc.).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":400,"error":"Bad Request","message":"newAllocation must be 0.00 or greater","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/categories","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "No valid Bearer token provided.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not own this budget.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":403,"error":"Forbidden","message":"You do not have permission to modify this budget","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/categories","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Budget not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":404,"error":"Not Found","message":"Budget not found: b1e2f3a4-5678-90ab-cdef-000000000001","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/categories","timestamp":"2026-05-16T12:00:00Z"}
                                    """)))
    })
    @PutMapping("/{budgetId}/categories")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> adjustCategory(
            @Parameter(
                    name = "budgetId",
                    in = ParameterIn.PATH,
                    description = "UUID of the target monthly budget.",
                    required = true,
                    example = "b1e2f3a4-5678-90ab-cdef-000000000001",
                    schema = @Schema(type = "string", format = "uuid")
            )
            @PathVariable UUID budgetId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Category name and new allocation amount.",
                    content = @Content(
                            schema = @Schema(implementation = CategoryAdjustDto.class),
                            examples = {
                                    @ExampleObject(name = "Update existing FOOD allocation",
                                            value = """
                                                    { "categoryName": "FOOD", "newAllocation": 500.00 }
                                                    """),
                                    @ExampleObject(name = "Add a custom GYM category",
                                            value = """
                                                    { "categoryName": "GYM", "newAllocation": 80.00 }
                                                    """)
                            }
                    )
            )
            @Valid @RequestBody CategoryAdjustDto dto,

            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        budgetService.adjustCategory(userDetails.getUserId(), budgetId, dto);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // DELETE /{budgetId}/categories/{name}
    // =========================================================================

    @Operation(
            summary = "Delete a budget category",
            description = """
                    Permanently removes a named category and all its associated transactions
                    (cascaded via the FK on the `transactions` table) from the specified budget.

                    The `name` path segment is **URL-encoded** — a category named `BOLT FOOD`
                    must be requested as `BOLT%20FOOD`. Matching is case-insensitive server-side.

                    **Ownership required:** the authenticated user must own this budget.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted successfully. No body returned.", content = @Content),
            @ApiResponse(responseCode = "401", description = "No valid Bearer token provided.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not own this budget.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":403,"error":"Forbidden","message":"You do not have permission to modify this budget","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/categories/FOOD","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Budget or category not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":404,"error":"Not Found","message":"Category 'SAVINGS' not found in budget b1e2f3a4-5678-90ab-cdef-000000000001","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/categories/SAVINGS","timestamp":"2026-05-16T12:00:00Z"}
                                    """)))
    })
    @DeleteMapping("/{budgetId}/categories/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteCategory(
            @Parameter(
                    name = "budgetId",
                    in = ParameterIn.PATH,
                    description = "UUID of the target monthly budget.",
                    required = true,
                    example = "b1e2f3a4-5678-90ab-cdef-000000000001",
                    schema = @Schema(type = "string", format = "uuid")
            )
            @PathVariable UUID budgetId,

            @Parameter(
                    name = "name",
                    in = ParameterIn.PATH,
                    description = "URL-encoded category name to delete. Matched case-insensitively.",
                    required = true,
                    example = "SAVINGS",
                    schema = @Schema(type = "string", maxLength = 50)
            )
            @PathVariable @Size(max = 50) String name,

            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        budgetService.deleteCategory(userDetails.getUserId(), budgetId, name);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // POST /transactions
    // =========================================================================

    @Operation(
            summary = "Log a manual spending transaction",
            description = """
                    Records a single manual expenditure against an existing budget category.

                    **Effect:**
                    1. Creates a row in the `transactions` table.
                    2. Increments `budget_categories.spent_amount` by the supplied `amount`.
                    3. The next `GET /current` will reflect the updated `spentAmount`, `remaining`,
                       `totalSpent`, `totalRemaining`, and `safeToSpendPerDay`.

                    The budget UUID and category name are provided in the request body.
                    Category name is matched case-insensitively and must already exist in the budget.

                    **Ownership required:** the authenticated user must own the target budget.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction logged and category spend updated. No body returned.", content = @Content),
            @ApiResponse(responseCode = "400", description = "Request body fails validation.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":400,"error":"Bad Request","message":"amount must be positive","path":"/api/v1/budgets/transactions","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "No valid Bearer token provided.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not own the referenced budget.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":403,"error":"Forbidden","message":"You do not have permission to modify this budget","path":"/api/v1/budgets/transactions","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Budget or category not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":404,"error":"Not Found","message":"Category 'GYM' not found in budget b1e2f3a4-5678-90ab-cdef-000000000001","path":"/api/v1/budgets/transactions","timestamp":"2026-05-16T12:00:00Z"}
                                    """)))
    })
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logManualTransaction(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Transaction details including the target budget, category, amount, and an optional description.",
                    content = @Content(
                            schema = @Schema(implementation = ManualTransactionRequestDto.class),
                            examples = {
                                    @ExampleObject(name = "Weekly grocery run",
                                            value = """
                                                    {
                                                      "budgetId":     "b1e2f3a4-5678-90ab-cdef-000000000001",
                                                      "categoryName": "FOOD",
                                                      "amount":       94.30,
                                                      "description":  "Kaufland — weekly groceries"
                                                    }
                                                    """),
                                    @ExampleObject(name = "Bus pass top-up",
                                            value = """
                                                    {
                                                      "budgetId":     "b1e2f3a4-5678-90ab-cdef-000000000001",
                                                      "categoryName": "TRANSPORT",
                                                      "amount":       70.00,
                                                      "description":  "STB monthly pass"
                                                    }
                                                    """)
                            }
                    )
            )
            @Valid @RequestBody ManualTransactionRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        budgetService.logManualTransaction(userDetails.getUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // POST /{budgetId}/upload-statement
    // =========================================================================

    @Operation(
            summary = "Upload and parse a bank statement CSV",
            description = """
                    Accepts a Revolut or ING CSV bank statement upload, keyword-routes each
                    debit row to a budget category, persists matched transactions, and returns
                    a per-row result list.

                    ## Supported CSV formats
                    The parser auto-detects column positions from the header row.
                    Supported header tokens (case-insensitive):
                    | Column type | Accepted header tokens |
                    |---|---|
                    | Description | `Description`, `Descriere`, `Merchant`, `Beneficiar`, `Narration`, `Details` |
                    | Amount | `Amount`, `Money Out`, `Debit`, `Suma`, `Withdrawal`, `Out` |

                    Quoted fields containing commas are handled correctly.
                    Income rows (zero or credit amounts) are silently skipped.

                    ## Keyword routing rules
                    | Keyword (substring, case-insensitive) | Category |
                    |---|---|
                    | kaufland, lidl, auchan, carrefour, mega image, glovo, bolt food, restaurant | FOOD |
                    | uber, bolt, stb, ratb, metrorex, petrom, omv, parking | TRANSPORT |
                    | chirie, rent, utilit, enel, romgaz, rcs, digi, orange, vodafone | HOUSING |
                    | farmaci, sensiblu, cosmet, salon | PERSONAL |
                    | cinema, netflix, spotify, hbo, club, bilet | LEISURE |
                    | librarie, papetarie, xerox | SUPPLIES |
                    | *(no match)* | PERSONAL (fallback) |

                    ## Persistence
                    A row is persisted only if the detected category already exists in the budget
                    (`persisted = true`). Unmatched categories appear in the response with
                    `persisted = false` — add the category via `PUT /{budgetId}/categories` and
                    re-upload to capture those rows.

                    **Ownership required:** the authenticated user must own this budget.

                    **Content-Type:** `multipart/form-data`, field name `file`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File parsed successfully. Returns one entry per debit row found.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ParsedTransactionDto.class)),
                            examples = @ExampleObject(
                                    name = "Revolut export — 3 rows parsed",
                                    value = """
                                            [
                                              { "description": "KAUFLAND IASI 2",     "amount": 94.32, "detectedCategory": "FOOD",      "persisted": true  },
                                              { "description": "UBER TRIP HELP",       "amount": 14.80, "detectedCategory": "TRANSPORT", "persisted": true  },
                                              { "description": "GYM WORLD CLASS IASI", "amount": 80.00, "detectedCategory": "PERSONAL",  "persisted": false }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "File is missing, empty, or cannot be parsed.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":400,"error":"Bad Request","message":"Failed to read bank statement: Stream closed","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/upload-statement","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "401", description = "No valid Bearer token provided.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not own this budget.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":403,"error":"Forbidden","message":"You do not have permission to modify this budget","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/upload-statement","timestamp":"2026-05-16T12:00:00Z"}
                                    """))),
            @ApiResponse(responseCode = "404", description = "Budget not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    {"status":404,"error":"Not Found","message":"Budget not found: b1e2f3a4-5678-90ab-cdef-000000000001","path":"/api/v1/budgets/b1e2f3a4-5678-90ab-cdef-000000000001/upload-statement","timestamp":"2026-05-16T12:00:00Z"}
                                    """)))
    })
    @PostMapping(value = "/{budgetId}/upload-statement", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ParsedTransactionDto>> uploadStatement(
            @Parameter(
                    name = "budgetId",
                    in = ParameterIn.PATH,
                    description = "UUID of the monthly budget to charge parsed transactions against.",
                    required = true,
                    example = "b1e2f3a4-5678-90ab-cdef-000000000001",
                    schema = @Schema(type = "string", format = "uuid")
            )
            @PathVariable UUID budgetId,

            @Parameter(
                    name = "file",
                    in = ParameterIn.QUERY,
                    description = "CSV bank statement file (UTF-8 encoded). Revolut and ING export formats are supported.",
                    required = true,
                    schema = @Schema(type = "string", format = "binary")
            )
            @RequestParam("file") MultipartFile file,

            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(budgetService.parseBankStatement(userDetails.getUserId(), budgetId, file));
    }
}
