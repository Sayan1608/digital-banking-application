package com.banking.accountservice.service.impl;

import com.banking.accountservice.dto.request.CreateAccountRequest;
import com.banking.accountservice.dto.response.AccountResponse;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountResponse createAccount(CreateAccountRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AccountResponse getAccount(UUID accountId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public BigDecimal getBalance(UUID accountId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AccountResponse deductBalance(UUID accountId, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AccountResponse creditBalance(UUID accountId, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
