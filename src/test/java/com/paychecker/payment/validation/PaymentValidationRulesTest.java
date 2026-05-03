package com.paychecker.payment.validation;

import com.paychecker.account.domain.Account;
import com.paychecker.account.domain.AccountStatus;
import com.paychecker.payment.dto.AuthorizePaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentValidationRulesTest {

    @Test
    void accountIsActiveRuleShouldReturnEmptyWhenAccountIsActive() {
        Account account = activeAccount();
        AuthorizePaymentRequest request = validRequest();

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        AccountIsActiveRule rule = new AccountIsActiveRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).isEmpty();
    }

    @Test
    void accountIsActiveRuleShouldReturnReasonWhenAccountIsBlocked() {
        Account account = activeAccount();
        account.setStatus(AccountStatus.BLOCKED);

        AuthorizePaymentRequest request = validRequest();

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        AccountIsActiveRule rule = new AccountIsActiveRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).contains("ACCOUNT_NOT_ACTIVE");
    }

    @Test
    void currencyMatchesRuleShouldReturnEmptyWhenCurrencyMatches() {
        Account account = activeAccount();
        AuthorizePaymentRequest request = validRequest();

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        CurrencyMatchesRule rule = new CurrencyMatchesRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).isEmpty();
    }

    @Test
    void currencyMatchesRuleShouldReturnReasonWhenCurrencyDoesNotMatch() {
        Account account = activeAccount();

        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                1L,
                new BigDecimal("100.00"),
                "USD",
                "PT50000200000000000000002",
                "Empresa XPTO",
                "PT"
        );

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        CurrencyMatchesRule rule = new CurrencyMatchesRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).contains("CURRENCY_MISMATCH");
    }

    @Test
    void sufficientBalanceRuleShouldReturnEmptyWhenBalanceIsEnough() {
        Account account = activeAccount();
        AuthorizePaymentRequest request = validRequest();

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        SufficientBalanceRule rule = new SufficientBalanceRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).isEmpty();
    }

    @Test
    void sufficientBalanceRuleShouldReturnReasonWhenBalanceIsNotEnough() {
        Account account = activeAccount();

        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                1L,
                new BigDecimal("2000.00"),
                "EUR",
                "PT50000200000000000000002",
                "Empresa XPTO",
                "PT"
        );

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        SufficientBalanceRule rule = new SufficientBalanceRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).contains("INSUFFICIENT_BALANCE");
    }

    @Test
    void paymentWithinDailyLimitRuleShouldReturnEmptyWhenAmountIsWithinLimit() {
        Account account = activeAccount();
        AuthorizePaymentRequest request = validRequest();

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        PaymentWithinDailyLimitRule rule = new PaymentWithinDailyLimitRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).isEmpty();
    }

    @Test
    void paymentWithinDailyLimitRuleShouldReturnReasonWhenAmountExceedsDailyLimit() {
        Account account = activeAccount();

        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                1L,
                new BigDecimal("600.00"),
                "EUR",
                "PT50000200000000000000002",
                "Empresa XPTO",
                "PT"
        );

        PaymentValidationContext context = new PaymentValidationContext(account, request);
        PaymentWithinDailyLimitRule rule = new PaymentWithinDailyLimitRule();

        Optional<String> result = rule.validate(context);

        assertThat(result).contains("DAILY_LIMIT_EXCEEDED");
    }

    private Account activeAccount() {
        return Account.builder()
                .id(1L)
                .ownerName("David Vieira")
                .iban("PT50000100000000000000001")
                .currency("EUR")
                .balance(new BigDecimal("1000.00"))
                .dailyLimit(new BigDecimal("500.00"))
                .monthlyLimit(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    private AuthorizePaymentRequest validRequest() {
        return new AuthorizePaymentRequest(
                1L,
                new BigDecimal("250.00"),
                "EUR",
                "PT50000200000000000000002",
                "Empresa XPTO",
                "PT"
        );
    }
}