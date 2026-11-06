package com.banking.accountservice.service.impl;

import com.banking.accountservice.dto.request.CreateAccountRequest;
import com.banking.accountservice.dto.response.AccountResponse;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import com.banking.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MAX_ACCOUNT_NUMBER = 1_000_000_000_000L;

    private final AccountRepository accountRepository;

    @Override
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Account exists with the given mail id: " + request.getEmail());
        }
        if (accountRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Account exists with the given phone number: " + request.getPhone());
        }

        String accountNumber = generateUniqueAccountNumber();
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(request.getAccountHolderName());
        account.setPhone(request.getPhone());
        account.setEmail(request.getEmail());
        account.setBalance(request.getInitialDepositAmount());
        account.setAccountType(request.getAccountType());
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setDailyTransactionLimit(calculateDailyLimit(request.getAccountType()));

        Account savedAccount = accountRepository.save(account);
        log.info("Created account {} for {}", savedAccount.getAccountNumber(), request.getAccountHolderName());

        return mapToResponse(savedAccount);
    }
    private String generateUniqueAccountNumber() {
        String accountNumber;
        do{
            long randomValue = RANDOM.nextLong(MAX_ACCOUNT_NUMBER);
            accountNumber = String.format("%012d", randomValue);
        }while(accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private BigDecimal calculateDailyLimit(AccountType accountType) {
        return accountType == AccountType.SAVINGS
                ? new BigDecimal("100000")
                : new BigDecimal("500000");
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getPhone(),
                account.getEmail(),
                account.getBalance(),
                account.getAccountType(),
                account.getAccountStatus(),
                account.getDailyTransactionLimit(),
                account.getCreatedAt()
        );
    }

    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Account doesn't exist with the provided account number: " + accountNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        return mapToResponse(findAccountOrThrow(accountNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNumber) {
        BigDecimal balance = findAccountOrThrow(accountNumber).getBalance();
        log.info("Fetched balance for account {}", accountNumber);
        return balance;
    }

    @Override
    public AccountResponse deductBalance(String accountNumber, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AccountResponse creditBalance(String accountNumber, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
