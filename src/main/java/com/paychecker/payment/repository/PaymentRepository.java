package com.paychecker.payment.repository;

import com.paychecker.payment.domain.Payment;
import com.paychecker.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByAccountId(Long accountId);

    List<Payment> findByStatus(PaymentStatus status);
}