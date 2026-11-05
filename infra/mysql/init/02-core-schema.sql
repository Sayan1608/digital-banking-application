-- ─────────────────────────────────────────────────────────
-- Minimal starting schema: double-entry ledger + outbox.
-- Replace with your migration tool (Flyway/Liquibase) when ready.
-- ─────────────────────────────────────────────────────────
USE dbank_accounts;

-- account-service owns this schema. The JPA `Account` entity is the source of
-- truth for the `accounts` table; Hibernate creates and evolves it at startup
-- (spring.jpa.hibernate.ddl-auto). Deliberately no DDL here, so there is one
-- definition of the table instead of two that drift apart.

USE dbank_ledger;

CREATE TABLE IF NOT EXISTS journal_entries (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  transaction_ref CHAR(36)        NOT NULL,
  -- Idempotency: the same client key can never post twice.
  idempotency_key CHAR(64)        NOT NULL,
  posted_at       TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  description     VARCHAR(255)    NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_journal_idempotency (idempotency_key),
  KEY idx_journal_ref (transaction_ref)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS journal_lines (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entry_id    BIGINT UNSIGNED NOT NULL,
  account_id  BIGINT UNSIGNED NOT NULL,
  direction   ENUM('DEBIT','CREDIT') NOT NULL,
  amount      DECIMAL(19,4)   NOT NULL,
  currency    CHAR(3)         NOT NULL DEFAULT 'USD',
  PRIMARY KEY (id),
  KEY idx_lines_entry (entry_id),
  KEY idx_lines_account (account_id),
  CONSTRAINT fk_lines_entry FOREIGN KEY (entry_id) REFERENCES journal_entries (id),
  CONSTRAINT ck_lines_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB;

USE dbank_payments;

-- Transactional outbox: services write business rows and the event
-- in one local transaction; a relay/CDC job ships them to Kafka.
CREATE TABLE IF NOT EXISTS outbox_events (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  aggregate_type VARCHAR(64)     NOT NULL,
  aggregate_id   VARCHAR(64)     NOT NULL,
  event_type     VARCHAR(64)     NOT NULL,
  topic          VARCHAR(128)    NOT NULL,
  payload        JSON            NOT NULL,
  headers        JSON            NULL,
  created_at     TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  published_at   TIMESTAMP(6)    NULL,
  PRIMARY KEY (id),
  KEY idx_outbox_unpublished (published_at, id),
  KEY idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB;
