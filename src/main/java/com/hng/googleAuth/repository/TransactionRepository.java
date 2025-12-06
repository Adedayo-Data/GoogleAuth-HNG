package com.hng.googleAuth.repository;

import com.hng.googleAuth.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByReference(String reference);

    boolean existsByReference(String reference);
}
