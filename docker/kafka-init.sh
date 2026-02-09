#!/bin/bash
set -e

echo "Creating Kafka topics for mod-entities-links..."

# Wait for Kafka to be ready
KAFKA_BROKER="${KAFKA_HOST}:${KAFKA_PORT}"
echo "Waiting for Kafka broker at $KAFKA_BROKER..."

until /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server "$KAFKA_BROKER" > /dev/null 2>&1; do
  echo "Kafka broker not ready yet, waiting..."
  sleep 2
done

echo "Kafka broker is ready!"

# Topics for mod-entities-links
TOPICS=(
"${ENV}.Default.inventory.instance"
"${ENV}.Default.inventory.holdings-record"
"${ENV}.Default.inventory.item"
"${ENV}.Default.inventory.bound-with"
"${ENV}.Default.authorities.authority"
"${ENV}.Default.authority.authority-source-file"
"${ENV}.Default.links.instance-authority"
"${ENV}.Default.links.instance-authority-stats"
"${ENV}.Default.DI_INVENTORY_AUTHORITY_UPDATED"
"${ENV}.Default.DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING"
"${ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_CREATED"
"${ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_MODIFIED_READY_FOR_POST_PROCESSING"
"${ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_NOT_MATCHED"
"${ENV}.Default.DI_SRS_MARC_AUTHORITY_RECORD_DELETED"
"${ENV}.Default.DI_COMPLETED"
"${ENV}.Default.DI_ERROR"
)

# Updated to use the full path to kafka-topics.sh
KAFKA_TOPICS_CMD="/opt/kafka/bin/kafka-topics.sh"

for TOPIC in "${TOPICS[@]}"; do
  $KAFKA_TOPICS_CMD \
    --create \
    --bootstrap-server "$KAFKA_BROKER" \
    --replication-factor 1 \
    --partitions "${KAFKA_TOPIC_PARTITIONS}" \
    --topic "$TOPIC" \
    --if-not-exists
  echo "Created topic: $TOPIC"
done

echo "Kafka topics created successfully."

