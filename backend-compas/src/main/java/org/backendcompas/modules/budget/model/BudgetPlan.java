package org.backendcompas.modules.budget.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "budget_plans")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class BudgetPlan {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "monthly_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "fixed_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedTotal;

    @Column(name = "disposable", nullable = false, precision = 12, scale = 2)
    private BigDecimal disposable;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "budget_plan_categories",
            joinColumns = @JoinColumn(name = "plan_id", nullable = false)
    )
    private List<BudgetPlanCategoryEntry> categories = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
