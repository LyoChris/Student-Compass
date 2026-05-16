package org.backendcompas.modules.budget.repository;

import org.backendcompas.modules.budget.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {}
