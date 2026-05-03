package com.paychecker.account.service;

import com.paychecker.account.domain.Account;
import com.paychecker.account.domain.AccountStatus;
import com.paychecker.account.dto.AccountResponse;
import com.paychecker.account.dto.CreateAccountRequest;
import com.paychecker.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.paychecker.eventlog.domain.EventType;
import com.paychecker.eventlog.service.EventLogService;
import com.paychecker.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final EventLogService eventLogService;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByIban(request.iban())) {
            throw new ResponseStatusException(CONFLICT, "An account with this IBAN already exists");
        }

        Account account = Account.builder()
                .ownerName(request.ownerName())
                .iban(request.iban())
                .currency(request.currency())
                .balance(request.initialBalance())
                .dailyLimit(request.dailyLimit())
                .monthlyLimit(request.monthlyLimit())
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        eventLogService.recordEvent(
                EventType.ACCOUNT_CREATED,
                "ACCOUNT",
                savedAccount.getId(),
                Map.of(
                        "iban", savedAccount.getIban(),
                        "currency", savedAccount.getCurrency(),
                        "status", savedAccount.getStatus().name()
                )
        );

        return toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> getAllAccounts(Pageable pageable) {
        return PageResponse.from(
                accountRepository.findAll(pageable)
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Account not found"));

        return toResponse(account);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerName(),
                account.getIban(),
                account.getCurrency(),
                account.getBalance(),
                account.getDailyLimit(),
                account.getMonthlyLimit(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}