package com.banking.accountservice;

import com.banking.accountservice.controller.AccountController;
import com.banking.accountservice.dto.request.CreateAccountRequest;
import com.banking.accountservice.dto.response.AccountResponse;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerIntegrationTest {

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/accounts creates a savings account with 13-digit account number and expected limits")
    void createSavingsAccount_shouldCreateWithExpectedRules() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                "Jane Doe",
                "+1 (555) 121-2121",
                "jane@example.com",
                AccountType.SAVINGS,
                new BigDecimal("2500.00")
        );

        AccountResponse response = new AccountResponse(
                UUID.randomUUID(),
                "0000001234567",
                "Jane Doe",
                "+1 (555) 121-2121",
                "jane@example.com",
                new BigDecimal("2500.00"),
                AccountType.SAVINGS,
                AccountStatus.ACTIVE,
                new BigDecimal("100000"),
                LocalDateTime.now()
        );

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountHolderName", is("Jane Doe")))
                .andExpect(jsonPath("$.accountStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.accountType", is("SAVINGS")))
                .andExpect(jsonPath("$.balance", is(2500.00)))
                .andExpect(jsonPath("$.dailyTransactionLimit", is(100000)))
                .andExpect(jsonPath("$.accountNumber", matchesPattern("\\d{13}")))
                .andExpect(jsonPath("$.accountNumber", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("POST /api/accounts creates a current account with 13-digit number and higher transaction limit")
    void createCurrentAccount_shouldCreateWithExpectedRules() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                "John Smith",
                "9876543210",
                "john@example.com",
                AccountType.CURRENT,
                new BigDecimal("12000.00")
        );

        AccountResponse response = new AccountResponse(
                UUID.randomUUID(),
                "0000009876543",
                "John Smith",
                "9876543210",
                "john@example.com",
                new BigDecimal("12000.00"),
                AccountType.CURRENT,
                AccountStatus.ACTIVE,
                new BigDecimal("500000"),
                LocalDateTime.now()
        );

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType", is("CURRENT")))
                .andExpect(jsonPath("$.accountStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.dailyTransactionLimit", is(500000)))
                .andExpect(jsonPath("$.accountNumber", matchesPattern("\\d{13}")));
    }

    @Test
    @DisplayName("POST /api/accounts rejects invalid payloads with validation errors")
    void createAccount_shouldRejectValidationErrors() throws Exception {
        String invalidPayload = "{\"accountHolderName\":\"\",\"phone\":\"abc\",\"email\":\"bad\",\"accountType\":\"SAVINGS\",\"initialDepositAmount\":0}";

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
