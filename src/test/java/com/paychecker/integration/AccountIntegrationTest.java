package com.paychecker.integration;

import com.paychecker.account.domain.AccountStatus;
import com.paychecker.account.dto.AccountResponse;
import com.paychecker.account.dto.CreateAccountRequest;
import com.paychecker.common.exception.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAccountSuccessfully() {
        CreateAccountRequest request = new CreateAccountRequest(
                "Integration User",
                "PT50010100000000000000101",
                "EUR",
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("5000.00")
        );

        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                "/api/accounts",
                request,
                AccountResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        AccountResponse account = response.getBody();

        assertThat(account.id()).isNotNull();
        assertThat(account.ownerName()).isEqualTo("Integration User");
        assertThat(account.iban()).isEqualTo("PT50010100000000000000101");
        assertThat(account.currency()).isEqualTo("EUR");
        assertThat(account.balance()).isEqualByComparingTo("1000.00");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldReturnConflictWhenIbanAlreadyExists() {
        CreateAccountRequest request = new CreateAccountRequest(
                "Duplicate User",
                "PT50010200000000000000102",
                "EUR",
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("5000.00")
        );

        restTemplate.postForEntity("/api/accounts", request, AccountResponse.class);

        ResponseEntity<ApiErrorResponse> duplicateResponse = restTemplate.postForEntity(
                "/api/accounts",
                request,
                ApiErrorResponse.class
        );

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateResponse.getBody()).isNotNull();
        assertThat(duplicateResponse.getBody().message())
                .isEqualTo("An account with this IBAN already exists");
    }

    @Test
    void shouldReturnValidationErrorsWhenRequestIsInvalid() {
        CreateAccountRequest request = new CreateAccountRequest(
                "",
                "",
                "EU",
                new BigDecimal("-10.00"),
                new BigDecimal("-5.00"),
                new BigDecimal("-100.00")
        );

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/accounts",
                request,
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        ApiErrorResponse error = response.getBody();

        assertThat(error.message()).isEqualTo("Validation failed");
        assertThat(error.validationErrors()).containsKeys(
                "ownerName",
                "iban",
                "currency",
                "initialBalance",
                "dailyLimit",
                "monthlyLimit"
        );
    }
}