/**
 * DTO to show response for account creation in bank
 */
package com.banking.accountservice.dto.response;

import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private UUID id;
    private String accountNumber;
    private String accountHolderName;
    private String phone;
    private String email;
    private BigDecimal balance;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private BigDecimal dailyTransactionLimit;
    private LocalDateTime createdAt;
}
