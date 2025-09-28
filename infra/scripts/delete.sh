#!/usr/bin/env bash
set -euo pipefail
CONTAINER=${CONTAINER:-rabbitmq-demo}
Q=${1:?Usage: delete.sh <queue-name>}
docker exec -it "$CONTAINER" rabbitmqctl delete_queue "$Q"
