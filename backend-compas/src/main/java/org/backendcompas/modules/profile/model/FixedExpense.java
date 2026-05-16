package org.backendcompas.modules.profile.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Embeddable value object representing a single fixed monthly expense.
 * Persisted in the {@code student_fixed_expenses} collection table.
 * Intentionally has no surrogate key — identity is determined by the owning profile.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedExpense {

    @Column(name = "expense_name", nullable = false, length = 100)
    private String name;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
