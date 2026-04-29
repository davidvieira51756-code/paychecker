package com.paychecker.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AuthorizePaymentRequest(

        @NotNull(message = "Account id is required")
        Long accountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        String currency,

        @NotBlank(message = "Beneficiary IBAN is required")
        @Size(max = 34, message = "Beneficiary IBAN must have at most 34 characters")
        String beneficiaryIban,

        @NotBlank(message = "Beneficiary name is required")
        @Size(max = 150, message = "Beneficiary name must have at most 150 characters")
        String beneficiaryName,

        @NotBlank(message = "Beneficiary country is required")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Beneficiary country must be a valid 2-letter country code")
        String beneficiaryCountry
) {
}