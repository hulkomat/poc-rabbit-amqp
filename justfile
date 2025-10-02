#!/usr/bin/env just --justfile

set dotenv-load

# Lists all available recipes if no recipe is specified
default:
    @just --list

start:
    # Start infra
    (cd infra && docker compose up -d)
    rm -rf logs && mkdir -p logs
    echo "Starting order-service & billing-service (detached)..."
    # Each line is a separate shell in just, so set LOG_ROOT inline per command.
    LOG_ROOT="$(pwd)/logs" ./gradlew :order-service:bootRun > logs/order-service.stdout.log 2>&1 &
    LOG_ROOT="$(pwd)/logs" ./gradlew :billing-service:bootRun > logs/billing-service.stdout.log 2>&1 &
    echo "Launched. Tail logs with: tail -f logs/order-service.stdout.log logs/billing-service.stdout.log"
    echo "Unified application log (both services): logs/application.log"
    echo "To stop, kill the Gradle bootRun processes, e.g.:"
    echo "  pkill -f order-service || true; pkill -f billing-service || true"


event:
    just start
    # Event
    curl -X POST localhost:8080/api/orders -H "content-type: application/json" \
        -d '{"orderId":"o-1","customerId":"c-1","amount":42.0}'

async-publish:
    just start
    # Async Publish (Publisher Confirms)
    curl -X POST localhost:8080/api/orders/async -H "content-type: application/json" \
        -d '{"orderId":"o-2","customerId":"c-1","amount":10.0}'

rpc:
    just start
    # RPC (async)
    curl -X POST localhost:8080/api/orders/billing/check-async -H "content-type: application/json" \
        -d '{"orderId":"o-3","customerId":"c-1","amount":12.34}'

immediate-dlq:
    just start
    # sofort DLQ (harte Exception)
    curl -X POST localhost:8080/api/orders -H "content-type: application/json" \
        -d '{"orderId":"o-err1","customerId":"c-1","amount":-1}'

non-retryable:
    just start
    # nicht-retriable Business-Fehler
    curl -X POST localhost:8080/api/orders -H "content-type: application/json" \
        -d '{"orderId":"o-err2","customerId":"c-bad","amount":10}'