#!/usr/bin/env bash
set -euo pipefail
ADMIN=infra/scripts/rabbitmqadmin
USER=${RABBIT_USER:-demo}
PASS=${RABBIT_PASS:-demo}
DLQ=${1:-q.order.created.dlq}
EX=${2:-ex.orders}
RK=${3:-order.created}
COUNT=${COUNT:-100}

# hole bis zu COUNT Messages aus der DLQ und republishe sie an den Exchange
for i in $(seq 1 $COUNT); do
  MSG=$($ADMIN -u "$USER" -p "$PASS" get queue="$DLQ" ackmode=ack_requeue_false requeue=false count=1)
  # wenn leer, abbrechen
  echo "$MSG" | grep -q "No messages" && { echo "DLQ empty"; exit 0; }

  # Roh-Payload extrahieren (quick&dirty; für echte Skripte JSON-Parsing nehmen)
  PAYLOAD=$(echo "$MSG" | awk -F"payload: " '/payload: /{print $2}' | head -n1)
  # PAYLOAD ist ohne Quotes; setze Properties als JSON
  $ADMIN -u "$USER" -p "$PASS" publish exchange="$EX" routing_key="$RK" payload="$PAYLOAD" properties='{"content_type":"application/json"}'
  echo "Republished: $PAYLOAD"
done
