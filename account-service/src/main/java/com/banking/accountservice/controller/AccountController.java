package com.banking.accountservice.controller;

import com.banking.accountservice.dto.request.CreateAccountRequest;
import com.banking.accountservice.dto.response.AccountResponse;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccount(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber) {
        BigDecimal balance = accountService.getBalance(accountNumber);
        return ResponseEntity.ok(balance);
    }

    /**
     * SAGA step 1
     * called by Transaction service when transaction is initiated
     */
    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<AccountResponse> deductBalance(@PathVariable String accountNumber,
                                                         @RequestParam BigDecimal amount) {
        AccountResponse response = accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok(response);
    }

    /**
     * SAGA step 4
     * Compensation Transaction End Point
     * called by Transaction Service when:
     *  1) Fraud detected -> refund sender (undo step 1)
     *  2) Transaction Completed -> credit receiver
     */
    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<AccountResponse> creditBalance(@PathVariable String accountNumber,
                                                         @RequestParam BigDecimal amount) {
        AccountResponse response = accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.ok(response);
    }
}
