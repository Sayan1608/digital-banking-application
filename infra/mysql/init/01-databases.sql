-- ─────────────────────────────────────────────────────────
-- Database-per-service. Each microservice owns its schema;
-- no cross-service joins, integration happens over Kafka.
-- ─────────────────────────────────────────────────────────
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS dbank_customers  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS dbank_accounts   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS dbank_ledger     CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS dbank_payments   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS dbank_cards      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS dbank_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS dbank_audit      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- The app user from .env gets full rights on every service schema.
GRANT ALL PRIVILEGES ON `dbank\_%`.* TO 'dbank'@'%';

-- Dedicated read-only user for reporting / BI.
CREATE USER IF NOT EXISTS 'dbank_ro'@'%' IDENTIFIED BY 'change_me_readonly';
GRANT SELECT, SHOW VIEW ON `dbank\_%`.* TO 'dbank_ro'@'%';

-- CDC user for Debezium-style outbox streaming into Kafka.
CREATE USER IF NOT EXISTS 'dbank_cdc'@'%' IDENTIFIED BY 'change_me_cdc';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT, LOCK TABLES
  ON *.* TO 'dbank_cdc'@'%';

FLUSH PRIVILEGES;
