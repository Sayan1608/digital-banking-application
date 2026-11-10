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

import static com.banking.accountservice.entity.AccountStatus.ACTIVE;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MAX_ACCOUNT_NUMBER = 1_000_000_000_000L;

    private final AccountRepository accountRepository;

    /**
     * To create new account for customer
     */
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
        account.setAccountStatus(ACTIVE);
        account.setDailyTransactionLimit(calculateDailyLimit(request.getAccountType()));

        Account savedAccount = accountRepository.save(account);
        log.info("Created account {} for {}", savedAccount.getAccountNumber(), request.getAccountHolderName());

        return mapToResponse(savedAccount);
    }

    /**
     * To create 12 digit unique account number
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        do{
            long randomValue = RANDOM.nextLong(MAX_ACCOUNT_NUMBER);
            accountNumber = String.format("%012d", randomValue);
        }while(accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    /**
     * To calculate daily transaction limit based on account type
     */
    private BigDecimal calculateDailyLimit(AccountType accountType) {
        return accountType == AccountType.SAVINGS
                ? new BigDecimal("100000")
                : new BigDecimal("500000");
    }

    /**
     * To map Account entity to AccountResponse DTO
     */
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

    /**
     * To find an account by its number or throw an exception if not found
     */
    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Account doesn't exist with the provided account number: " + accountNumber));
    }

    /**
     * To validate that a transaction amount is present and strictly positive
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transaction amount must be greater than zero, but was: " + amount);
        }
    }

    /**
     * To get customer account details by account number
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        return mapToResponse(findAccountOrThrow(accountNumber));
    }

    /**
     * To get customer account balance by account number
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNumber) {
        BigDecimal balance = findAccountOrThrow(accountNumber).getBalance();
        log.info("Fetched balance for account {}", accountNumber);
        return balance;
    }

    /**
     * To block a customer's account.
     * Called by the Fraud Detection service when a fraudulent transaction is detected.
     * Idempotent: blocking an already blocked account is a no-op.
     */
    @Override
    @Transactional
    public AccountResponse blockAccount(String accountNumber) {
        Account account = findAccountOrThrow(accountNumber);

        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new RuntimeException("Account is closed and cannot be blocked: " + accountNumber);
        }

        if (account.getAccountStatus() == AccountStatus.BLOCKED) {
            log.info("Account {} is already blocked, skipping", accountNumber);
            return mapToResponse(account);
        }

        account.setAccountStatus(AccountStatus.BLOCKED);
        Account blockedAccount = accountRepository.save(account);
        log.warn("Blocked account {} due to suspected fraud", accountNumber);

        return mapToResponse(blockedAccount);
    }

    /**
     * To deduct balance from a customer's account
     * Called by the Transaction service when a transaction is initiated.
     */
    @Override
    @Transactional
    public AccountResponse deductBalance(String accountNumber, BigDecimal amount) {
        log.info("Attempting to deduct {} from account {}", amount, accountNumber);
        validateAmount(amount);
        Account account = findAccountOrThrow(accountNumber);

        if(!ACTIVE.equals(account.getAccountStatus())) {
            throw new RuntimeException("Account is not active " + accountNumber);
        }

        if(account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in account " + accountNumber);
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        Account updatedAccount = accountRepository.save(account);
        log.info("Deducted {} from account {}", amount, accountNumber);
        return mapToResponse(updatedAccount);
    }

    /**
     * To credit balance into a customer's account.
     * called by the Transaction Service from kafka
     * Called by the Transaction service when funds are received.
     * Only an ACTIVE account can be credited.
     */
    @Override
    @Transactional
    public AccountResponse creditBalance(String accountNumber, BigDecimal amount) {
        log.info("Attempting to credit {} to account {}", amount, accountNumber);
        validateAmount(amount);
        Account account = findAccountOrThrow(accountNumber);

        if (!ACTIVE.equals(account.getAccountStatus())) {
            log.warn("Credit rejected for account {}: account status is {}", accountNumber, account.getAccountStatus());
            throw new RuntimeException("Account is not active " + accountNumber);
        }

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        Account updatedAccount = accountRepository.save(account);
        log.info("Credited {} to account {}", amount, accountNumber);
        return mapToResponse(updatedAccount);
    }
}
