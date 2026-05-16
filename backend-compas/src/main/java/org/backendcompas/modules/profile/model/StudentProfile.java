package org.backendcompas.modules.profile.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Financial onboarding profile for a student.
 *
 * Design: the PK {@code userId} is the same UUID as the user record in the {@code users} table.
 * This enforces a strict 1-to-1 relationship at the database level without a separate join column,
 * and makes all look-ups O(1) by the auth principal's ID with no extra join.
 */
@Entity
@Table(name = "student_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class StudentProfile {

    /** Primary key — mirrors the UUID of the authenticated user. */
    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "living_area", nullable = false, length = 30)
    private LivingArea livingArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "eating_habit", nullable = false, length = 30)
    private EatingHabit eatingHabit;

    @Enumerated(EnumType.STRING)
    @Column(name = "home_package_frequency", nullable = false, length = 30)
    private HomePackageFrequency homePackageFrequency;

    @Column(name = "monthly_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyBudget;

    /**
     * Fixed monthly expenses stored in a dedicated child table.
     *
     * On upsert the service calls {@code clear()} + {@code addAll()} so Hibernate performs
     * a full DELETE + re-INSERT of the collection rows — correct for the "replace all" semantic.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "student_fixed_expenses",
            joinColumns = @JoinColumn(
                    name = "profile_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_fixed_expenses_profile")
            )
    )
    @OrderBy("expense_name ASC")
    private List<FixedExpense> fixedExpenses = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
