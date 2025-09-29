#!/usr/bin/env bash
set -euo pipefail
CONTAINER=${CONTAINER:-rabbitmq-demo}
docker exec -it "$CONTAINER" rabbitmqctl list_queues name messages arguments
