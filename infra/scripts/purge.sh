#!/usr/bin/env bash
set -euo pipefail
CONTAINER=${CONTAINER:-rabbitmq-demo}
Q=${1:?Usage: purge.sh <queue-name>}
docker exec -it "$CONTAINER" rabbitmqctl purge_queue "$Q"
