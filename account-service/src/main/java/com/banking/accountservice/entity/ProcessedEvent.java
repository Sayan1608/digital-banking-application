package com.banking.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Inbox of Kafka events this service has already applied.
 *
 * Kafka delivery is at-least-once, so the same event can arrive more than once
 * (consumer rebalance, a failed offset commit after a successful DB commit).
 * Crediting a balance is not naturally idempotent, so a redelivery would credit
 * the receiver twice. The consumer writes the event id here in the same
 * transaction as the balance change: either both commit or neither does, and a
 * replay is recognised and skipped.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
public class ProcessedEvent {

    /**
     * The producer-assigned event id, used directly as the primary key so the
     * database rejects a duplicate even if two consumers race.
     */
    @Id
    @Column(nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(length = 32)
    private String accountNumber;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(String eventId, String eventType, String accountNumber) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.accountNumber = accountNumber;
    }
}
