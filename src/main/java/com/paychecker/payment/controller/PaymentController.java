package com.paychecker.payment.controller;

import com.paychecker.payment.dto.AuthorizePaymentRequest;
import com.paychecker.payment.dto.PaymentAuthorizationResponse;
import com.paychecker.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/authorize")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentAuthorizationResponse authorizePayment(@Valid @RequestBody AuthorizePaymentRequest request) {
        return paymentService.authorizePayment(request);
    }
}