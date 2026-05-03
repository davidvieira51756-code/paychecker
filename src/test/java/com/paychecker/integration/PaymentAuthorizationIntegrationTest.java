package com.paychecker.integration;

import com.paychecker.account.dto.AccountResponse;
import com.paychecker.account.dto.CreateAccountRequest;
import com.paychecker.alert.dto.RiskAlertResponse;
import com.paychecker.common.dto.PageResponse;
import com.paychecker.eventlog.dto.FinancialEventResponse;
import com.paychecker.payment.domain.PaymentStatus;
import com.paychecker.payment.dto.AuthorizePaymentRequest;
import com.paychecker.payment.dto.PaymentAuthorizationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentAuthorizationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldSendHighRiskPaymentToManualReviewAndCreateAlertAndEvents() {
        AccountResponse account = createAccount();

        AuthorizePaymentRequest paymentRequest = new AuthorizePaymentRequest(
                account.id(),
                new BigDecimal("6000.00"),
                "EUR",
                "PT50008800000000000000088",
                "New Large Beneficiary",
                "PT"
        );

        ResponseEntity<PaymentAuthorizationResponse> paymentResponse =
                restTemplate.postForEntity(
                        "/api/payments/authorize",
                        paymentRequest,
                        PaymentAuthorizationResponse.class
                );

        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(paymentResponse.getBody()).isNotNull();

        PaymentAuthorizationResponse payment = paymentResponse.getBody();

        assertThat(payment.paymentId()).isNotNull();
        assertThat(payment.decision()).isEqualTo(PaymentStatus.MANUAL_REVIEW);
        assertThat(payment.riskScore()).isEqualTo(75);
        assertThat(payment.reasons()).containsExactly("VERY_HIGH_AMOUNT", "NEW_BENEFICIARY");

        ResponseEntity<PageResponse<RiskAlertResponse>> alertsResponse =
                restTemplate.exchange(
                        "/api/alerts?page=0&size=10",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        }
                );

        assertThat(alertsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(alertsResponse.getBody()).isNotNull();
        assertThat(alertsResponse.getBody().content()).hasSize(1);

        RiskAlertResponse alert = alertsResponse.getBody().content().getFirst();

        assertThat(alert.paymentId()).isEqualTo(payment.paymentId());
        assertThat(alert.accountId()).isEqualTo(account.id());
        assertThat(alert.riskScore()).isEqualTo(75);
        assertThat(alert.status().name()).isEqualTo("OPEN");
        assertThat(alert.severity().name()).isEqualTo("HIGH");

        ResponseEntity<PageResponse<FinancialEventResponse>> eventsResponse =
                restTemplate.exchange(
                        "/api/event-log?page=0&size=10",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        }
                );

        assertThat(eventsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(eventsResponse.getBody()).isNotNull();

        assertThat(eventsResponse.getBody().content())
                .extracting(event -> event.eventType().name())
                .contains(
                        "ACCOUNT_CREATED",
                        "PAYMENT_REQUESTED",
                        "PAYMENT_SENT_TO_REVIEW",
                        "RISK_ALERT_CREATED"
                );
    }

    private AccountResponse createAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "Integration Payment User",
                "PT50020100000000000000201",
                "EUR",
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("50000.00")
        );

        ResponseEntity<AccountResponse> response =
                restTemplate.postForEntity("/api/accounts", request, AccountResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }
}