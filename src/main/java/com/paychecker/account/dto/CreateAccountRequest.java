package com.paychecker.account.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Owner name is required")
        @Size(max = 150, message = "Owner name must have at most 150 characters")
        String ownerName,

        @NotBlank(message = "IBAN is required")
        @Size(max = 34, message = "IBAN must have at most 34 characters")
        String iban,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
        String currency,

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
        BigDecimal initialBalance,

        @NotNull(message = "Daily limit is required")
        @DecimalMin(value = "0.00", message = "Daily limit cannot be negative")
        BigDecimal dailyLimit,

        @NotNull(message = "Monthly limit is required")
        @DecimalMin(value = "0.00", message = "Monthly limit cannot be negative")
        BigDecimal monthlyLimit
) {
}