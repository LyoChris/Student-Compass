package org.backendcompas.modules.budget.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.backendcompas.core.exception.ApiError;
import org.backendcompas.modules.budget.dto.BudgetPlanResponseDto;
import org.backendcompas.modules.budget.service.BudgetService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Budget",
        description = """
                Deterministic monthly spending plan derived from the student's financial profile.

                ## How the plan is computed
                The engine never calls an LLM — it applies a fixed weight matrix to the student's
                **disposable income** (`monthlyBudget − fixedExpenses`) and then adjusts multipliers
                based on the student's profile:

                | Profile signal | Effect |
                |---|---|
                | `eatingHabit = COOKING` | FOOD × 0.85 |
                | `eatingHabit = EATING_OUT / DELIVERY` | FOOD × 1.25 |
                | `livingArea = DORMITORY` | HOUSING × 0.40, TRANSPORT × 0.70 |
                | `livingArea = RENT` | HOUSING × 1.30 |
                | `livingArea = COMMUTER` | TRANSPORT × 1.60 |
                | `homePackageFrequency = WEEKLY` | FOOD × 0.80 |
                | `homePackageFrequency = NONE` | FOOD × 1.10 |

                After applying all multipliers, weights are **renormalized** to sum to 1.0 before
                being multiplied by `disposable`. Fixed expenses are passed through verbatim in the
                response alongside the category breakdown.

                ## Auto-recompute
                The plan is automatically recomputed every time the student's profile is created or
                updated via `PUT /api/v1/profiles/{userId}`. A manual recompute endpoint is also
                available for cases where an admin has changed the profile.

                ## Access rules
                A student may only read/recompute their **own** plan.
                An **ADMIN** may access or recompute any student's plan.
                """
)
@RestController
@RequestMapping("/api/v1/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // -------------------------------------------------------------------------
    // GET /{userId}
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get the current budget plan",
            description = """
                    Returns the latest deterministic spending plan stored for the given user.

                    The plan is composed of:
                    - `monthlyBudget` — the total budget declared in the student's profile
                    - `fixedTotal` — sum of all fixed monthly expenses (rent, subscriptions, …)
                    - `disposable` — `monthlyBudget − fixedTotal` (floored at 0 if negative)
                    - `categories` — per-category allocation of `disposable` (FOOD, TRANSPORT, …)
                    - `fixedExpenses` — verbatim fixed expense list from the profile

                    Returns **404** if the student has never submitted a profile (and therefore no
                    plan has ever been computed). Ask the student to complete onboarding first, or
                    call `POST /api/v1/budget/{userId}/recompute` after the profile is in place.

                    **Access rules:**
                    - A student may only fetch **their own** plan (`userId` must match the JWT subject).
                    - An **ADMIN** may fetch any student's plan.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Budget plan found and returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BudgetPlanResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Student on rent, cooking, monthly package",
                                    value = """
                                            {
                                              "planId": "c3d4e5f6-1234-5678-abcd-000000000001",
                                              "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                              "monthlyBudget": 1500.00,
                                              "fixedTotal": 400.00,
                                              "disposable": 1100.00,
                                              "categories": [
                                                { "category": "FOOD",      "amount": 330.73 },
                                                { "category": "HOUSING",   "amount": 117.34 },
                                                { "category": "LEISURE",   "amount": 96.55  },
                                                { "category": "PERSONAL",  "amount": 96.55  },
                                                { "category": "SAVINGS",   "amount": 144.83 },
                                                { "category": "SUPPLIES",  "amount": 77.24  },
                                                { "category": "TRANSPORT", "amount": 116.76 }
                                              ],
                                              "fixedExpenses": [
                                                { "name": "Chirie",   "amount": 350.00 },
                                                { "name": "Internet", "amount": 50.00  }
                                              ],
                                              "updatedAt": "2026-05-16T10:00:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No valid Bearer token was provided in the `Authorization` header",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 401, "error": "Unauthorized", "message": "Full authentication is required" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "The authenticated user is neither the plan owner nor an ADMIN",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 403, "error": "Forbidden", "message": "Access denied" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No budget plan exists for this user — the student has not completed onboarding yet",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 404, "error": "Not Found", "message": "No budget plan found for user: 3fa85f64-5717-4562-b3fc-2c963f66afa6" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.user.id or hasRole('ADMIN')")
    public ResponseEntity<BudgetPlanResponseDto> getPlan(
            @Parameter(
                    description = "UUID of the user whose budget plan to retrieve. Must match the authenticated user's ID unless the caller is an ADMIN.",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    required = true
            )
            @PathVariable UUID userId) {
        return ResponseEntity.ok(budgetService.getPlan(userId));
    }

    // -------------------------------------------------------------------------
    // POST /{userId}/recompute
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Recompute the budget plan from the current profile",
            description = """
                    Reads the student's **current** financial profile and derives a fresh deterministic
                    spending plan using the weight matrix described in the tag description above.

                    **Upsert semantics:**
                    - If no plan exists yet, a new `budget_plans` row is created.
                    - If a plan already exists, all amounts are overwritten in-place (same `planId`).

                    This endpoint is called automatically by the profile upsert
                    (`PUT /api/v1/profiles/{userId}`) so in most cases the frontend does not need to
                    call it explicitly. Use it if you need to force a refresh after an admin changes
                    the profile directly.

                    Returns **404** if the user has not yet submitted a financial profile — the student
                    must complete onboarding before a plan can be computed.

                    **Access rules:**
                    - A student may only recompute **their own** plan.
                    - An **ADMIN** may recompute any student's plan.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Plan recomputed and returned. Category amounts reflect the profile state at the time of this call.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BudgetPlanResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Student in dorm, eating in canteen, bi-weekly package",
                                    value = """
                                            {
                                              "planId": "c3d4e5f6-1234-5678-abcd-000000000001",
                                              "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                              "monthlyBudget": 900.00,
                                              "fixedTotal": 0.00,
                                              "disposable": 900.00,
                                              "categories": [
                                                { "category": "FOOD",      "amount": 315.00 },
                                                { "category": "HOUSING",   "amount": 36.00  },
                                                { "category": "LEISURE",   "amount": 90.00  },
                                                { "category": "PERSONAL",  "amount": 90.00  },
                                                { "category": "SAVINGS",   "amount": 135.00 },
                                                { "category": "SUPPLIES",  "amount": 72.00  },
                                                { "category": "TRANSPORT", "amount": 162.00 }
                                              ],
                                              "fixedExpenses": [],
                                              "updatedAt": "2026-05-16T11:30:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No valid Bearer token was provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 401, "error": "Unauthorized", "message": "Full authentication is required" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "The authenticated user is neither the plan owner nor an ADMIN",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 403, "error": "Forbidden", "message": "Access denied" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "The user has no financial profile — they must complete onboarding first",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "status": 404, "error": "Not Found", "message": "No profile found for user: 3fa85f64-5717-4562-b3fc-2c963f66afa6" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    @PostMapping("/{userId}/recompute")
    @PreAuthorize("#userId == authentication.principal.user.id or hasRole('ADMIN')")
    public ResponseEntity<BudgetPlanResponseDto> recompute(
            @Parameter(
                    description = "UUID of the user whose plan to recompute. Must match the authenticated user's ID unless the caller is an ADMIN.",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    required = true
            )
            @PathVariable UUID userId) {
        return ResponseEntity.ok(budgetService.recompute(userId));
    }
}
