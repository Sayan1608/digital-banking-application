package com.banking.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Consumes banking events that the account service has to react to.
 *
 * Payloads are read as a plain Map so this service does not need a copy of the
 * producing service's event classes. The trade-off is that field names are a
 * contract agreed by convention, not by a shared type, so a rename upstream
 * shows up here as a missing-field error rather than a compile error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventConsumer {

    private final AccountService accountService;

    /**
     * Credits the receiver once the Transaction service reports a completed transfer.
     *
     * The debit of the sender already happened synchronously during the transfer,
     * so this listener only applies the credit half.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.transaction-completed}",
            groupId = "account-service")
    public void onTransactionCompleted(Map<String, Object> event) {
        log.info("Received transaction.completed event: {}", event);

        String receiverAccountNumber = requireString(event, "receiverAccountNumber");
        BigDecimal amount = requireAmount(event, "amount");

        accountService.creditBalance(receiverAccountNumber, amount);
        log.info("Applied transaction.completed: credited {} to receiver account {}",
                amount, receiverAccountNumber);
    }

    /**
     * Blocks an account once the Fraud Detection service flags it.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.fraud-detected}",
            groupId = "account-service")
    public void onFraudDetected(Map<String, Object> event) {
        log.info("Received fraud.detected event: {}", event);

        String accountNumber = requireString(event, "accountNumber");

        accountService.blockAccount(accountNumber);
        log.warn("Applied fraud.detected: blocked account {}", accountNumber);
    }

    /**
     * Reads a required text field. Throws IllegalArgumentException, which the
     * error handler treats as non-retryable: replaying a malformed event
     * cannot make the field appear.
     */
    private String requireString(Map<String, Object> event, String field) {
        Object value = event.get(field);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Event is missing required field '" + field + "': " + event);
        }
        return value.toString();
    }

    /**
     * Reads a required money field. JSON numbers arrive as Integer/Double/BigDecimal
     * depending on the value, so it is parsed from its text form to avoid
     * binary floating point creeping into a balance.
     */
    private BigDecimal requireAmount(Map<String, Object> event, String field) {
        String raw = requireString(event, field);
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Event field '" + field + "' is not a number: " + raw, e);
        }
    }
}
