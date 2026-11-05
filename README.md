# Digital Banking System — Local Infrastructure

Docker Compose stack: **MySQL 8**, **Redis 7**, **ZooKeeper**, **Kafka** (+ optional Kafka UI).

## Quick start

```bash
docker compose up -d          # start everything
docker compose ps             # all four should read (healthy)
docker compose logs -f kafka  # follow a service
docker compose down           # stop (volumes kept)
docker compose down -v        # stop and wipe all data
```

Topics are provisioned by the one-shot `kafka-init` container, which exits 0 when done.

Optional Kafka UI on <http://localhost:8080>:

```bash
docker compose --profile tools up -d
```

## Connection details

| Service   | From host             | From another container | Credentials                   |
|-----------|-----------------------|------------------------|-------------------------------|
| MySQL     | `localhost:3307`      | `mysql:3306`           | `dbank` / `change_me_app`     |
| Redis     | `localhost:6379`      | `redis:6379`           | password `change_me_redis`    |
| Kafka     | `localhost:29092`     | `kafka:9092`           | PLAINTEXT, no auth            |
| ZooKeeper | `localhost:2181`      | `zookeeper:2181`       | none                          |

> **MySQL is on host port 3307, not 3306.** A native `mysqld` service already owns
> 3306 on this machine. Change `MYSQL_PORT` in `.env` if you free that port up.

Kafka advertises two listeners. Use `kafka:9092` from inside the network and
`localhost:29092` from your IDE or host-run app — mixing them up is the usual
cause of "connected, then timed out".

## Databases

One schema per service, no cross-service joins — services integrate over Kafka:

`dbank_customers`, `dbank_accounts`, `dbank_ledger`, `dbank_payments`,
`dbank_cards`, `dbank_notifications`, `dbank_audit`

Extra users: `dbank_ro` (read-only reporting), `dbank_cdc` (Debezium/CDC replication).

Starter tables in `infra/mysql/init/02-core-schema.sql`: `accounts`,
`journal_entries` + `journal_lines` (double-entry), `outbox_events`.
Init scripts run **only on first start** with an empty volume — after that, use
a migration tool (Flyway/Liquibase) or `docker compose down -v` to re-seed.

## Topics

| Topic | Parts | Policy | Retention |
|---|---|---|---|
| `customer.events` | 6 | delete | 30d |
| `account.events` | 6 | delete | 30d |
| `account.balance.snapshot` | 6 | **compact** | ∞ |
| `payment.commands` | 12 | delete | 14d |
| `payment.events` | 12 | delete | 30d |
| `transfer.saga.events` | 12 | delete | 14d |
| `ledger.postings` | 12 | delete | **∞** |
| `card.authorization.events` | 6 | delete | 14d |
| `fraud.alerts` | 3 | delete | 90d |
| `notification.requests` | 3 | delete | 7d |
| `audit.trail` | 6 | delete | **∞** |
| `payment.events.dlq`, `payment.commands.dlq` | 3 | delete | 90d |

Add or change topics in `infra/kafka/create-topics.sh`, then re-run:

```bash
docker compose up kafka-init --force-recreate
```

Partition by account ID so all events for one account stay ordered on one partition.

## Choices worth knowing

**MySQL** — `READ-COMMITTED`, `innodb-flush-log-at-trx-commit=1` and `sync-binlog=1`:
every commit is durable on disk before acknowledgement. Row-format binlog with GTIDs
is on, so Debezium CDC and replicas work without reconfiguring. Money is
`DECIMAL(19,4)`, never float. `accounts.version` supports optimistic locking.

**Kafka** — `auto.create.topics.enable=false` and `unclean.leader.election.enable=false`.
Topics are declared explicitly, and a lagging replica can never be elected leader
and silently lose acknowledged writes.

**Redis** — `maxmemory-policy noeviction` with AOF `everysec`. Eviction would drop
idempotency keys and let a payment post twice, so Redis rejects writes instead of
discarding data. `FLUSHALL`/`FLUSHDB`/`KEYS` are renamed to nothing.

## Not production-ready

This is a local development stack. Before any shared environment:

- Replace every `change_me_*` secret in `.env`; it is gitignored, `.env.example` is the template.
- No TLS or SASL anywhere — Kafka and Redis accept any client on the network.
- Single Kafka broker, `replication-factor=1`: no redundancy. Production needs 3+
  brokers, RF=3, `min.insync.replicas=2` and `acks=all`.
- Single MySQL with no replica or backup job.
- ZooKeeper is a single node. Also note it is deprecated in current Kafka —
  KRaft is the path forward when you consolidate.
