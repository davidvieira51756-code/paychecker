package com.paychecker.alert.repository;

import com.paychecker.alert.domain.RiskAlert;
import com.paychecker.alert.domain.RiskAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    Page<RiskAlert> findByStatus(RiskAlertStatus status, Pageable pageable);

    List<RiskAlert> findByAccount_Id(Long accountId);

    List<RiskAlert> findByPayment_Id(Long paymentId);
}