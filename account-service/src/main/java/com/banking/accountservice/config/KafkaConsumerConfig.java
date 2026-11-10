package com.banking.accountservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer wiring for the account service.
 *
 * Events are consumed as plain Maps, so there is no per-event-type container
 * factory here; the deserializer is configured in application.yaml to read
 * every payload as a Map and to ignore the producer's __TypeId__ header, so
 * this service neither depends on that header nor lets it choose a class.
 *
 * Spring Boot applies the error handler below to the auto-configured listener
 * container factory, so every @KafkaListener in this service picks it up.
 */
@Configuration
@Slf4j
public class KafkaConsumerConfig {

    /**
     * Retries a failed record three times with a 2s pause, then gives up on it
     * so one poison record cannot stall its partition forever. A malformed
     * payload is not retried at all, since replaying it cannot help.
     *
     * NOTE: the exhausted record is logged and skipped. There is no DLQ topic
     * provisioned for these events yet (see infra/kafka/create-topics.sh), so
     * recovery is currently "read the error log".
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Giving up on event from topic {} partition {} offset {} after retries; "
                                + "it will NOT be applied: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage(), exception),
                new FixedBackOff(2000L, 3L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
