#!/bin/bash
# Provisions the banking event topics. Auto-create is disabled on the
# broker, so topics are declared here explicitly and idempotently.
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka:9092}"
RF=1   # single-broker dev cluster

echo "waiting for kafka at ${BOOTSTRAP}..."
until kafka-broker-api-versions --bootstrap-server "${BOOTSTRAP}" >/dev/null 2>&1; do
  sleep 2
done
echo "kafka is up"

# name:partitions:cleanup.policy:retention_ms
TOPICS=(
  "customer.events:6:delete:2592000000"
  "account.events:6:delete:2592000000"
  "account.balance.snapshot:6:compact:-1"
  "payment.commands:12:delete:1209600000"
  "payment.events:12:delete:2592000000"
  "transfer.saga.events:12:delete:1209600000"
  "ledger.postings:12:delete:-1"
  "card.authorization.events:6:delete:1209600000"
  "fraud.alerts:3:delete:7776000000"
  "notification.requests:3:delete:604800000"
  "audit.trail:6:delete:-1"
  "payment.events.dlq:3:delete:7776000000"
  "payment.commands.dlq:3:delete:7776000000"
)

for spec in "${TOPICS[@]}"; do
  IFS=':' read -r name parts policy retention <<< "$spec"
  echo "→ ${name} (partitions=${parts} policy=${policy} retention=${retention})"
  kafka-topics --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists \
    --topic "${name}" \
    --partitions "${parts}" \
    --replication-factor "${RF}" \
    --config "cleanup.policy=${policy}" \
    --config "retention.ms=${retention}" \
    --config "min.insync.replicas=1"
done

echo ""
echo "topics now present:"
kafka-topics --bootstrap-server "${BOOTSTRAP}" --list
echo "topic provisioning complete"
