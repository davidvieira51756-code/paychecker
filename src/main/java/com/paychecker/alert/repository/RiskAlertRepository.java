package com.paychecker.alert.repository;

import com.paychecker.alert.domain.RiskAlert;
import com.paychecker.alert.domain.RiskAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    List<RiskAlert> findByStatus(RiskAlertStatus status);

    List<RiskAlert> findByAccount_Id(Long accountId);

    List<RiskAlert> findByPayment_Id(Long paymentId);
}