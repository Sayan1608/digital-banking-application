package com.banking.accountservice.service;

import com.banking.accountservice.dto.request.CreateAccountRequest;
import com.banking.accountservice.dto.response.AccountResponse;

import java.math.BigDecimal;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(String accountNumber);

    BigDecimal getBalance(String accountNumber);

    AccountResponse blockAccount(String accountNumber);

    AccountResponse deductBalance(String accountNumber, BigDecimal amount);

    AccountResponse creditBalance(String accountNumber, BigDecimal amount);
}
