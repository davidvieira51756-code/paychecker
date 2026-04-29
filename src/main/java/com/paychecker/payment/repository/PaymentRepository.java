package com.paychecker.payment.repository;

import com.paychecker.payment.domain.Payment;
import com.paychecker.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByAccount_Id(Long accountId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByAccount_IdAndBeneficiaryIban(Long accountId, String beneficiaryIban);

    long countByAccount_IdAndCreatedAtAfter(Long accountId, Instant createdAt);
}