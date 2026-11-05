/**
 * DTO to create customer account in bank
 */
package com.banking.accountservice.dto.request;

import com.banking.accountservice.entity.AccountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Phone number must be a valid format")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be in a valid format")
    private String email;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Initial deposit amount is required")
    @Positive( message = "Initial deposit amount must be greater than zero")
    private BigDecimal initialDepositAmount;

}
