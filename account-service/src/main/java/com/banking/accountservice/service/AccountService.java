package com.banking.accountservice.service;

import com.banking.accountservice.dto.request.CreateAccountRequest;
import com.banking.accountservice.dto.response.AccountResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(UUID accountId);

    BigDecimal getBalance(UUID accountId);

    AccountResponse deductBalance(UUID accountId, BigDecimal amount);

    AccountResponse creditBalance(UUID accountId, BigDecimal amount);
}
