# RabbitMQ AMQP Demo (Spring Boot, Gradle, Java 21)

Mono-Repo mit zwei Services und RabbitMQ via Docker Compose.

## Struktur
- `infra/` – Docker Compose & Skripte
- `common-dto/` – gemeinsame DTOs
- `order-service/` – Producer (+ async publish mit Publisher Confirms, RPC-Client)
- `billing-service/` – Consumer (Queue/Binding, Retry + DLQ, RPC-Handler)
- `docs/` – Cheatsheet & Best Practices

## Start
```bash
cd infra
docker compose up -d
# UI: http://localhost:15672  (demo/demo)
```

In zwei Terminals:
```bash
./gradlew :billing-service:bootRun
./gradlew :order-service:bootRun
```

## Test: Event publish
```bash
curl -X POST localhost:8080/api/orders   -H "content-type: application/json"   -d '{"orderId":"o-1","customerId":"c-1","amount":42.0}'
```

## Test: Async publish (Publisher Confirms)
```bash
curl -X POST localhost:8080/api/orders/async   -H "content-type: application/json"   -d '{"orderId":"o-2","customerId":"c-1","amount":10.0}'
```

## Test: RPC (Request/Reply, async)
```bash
curl -X POST localhost:8080/api/orders/billing/check-async   -H "content-type: application/json"   -d '{"orderId":"o-3","customerId":"c-1","amount":12.34}'
```

## DLQ provozieren
```bash
# negative amount -> sofort DLQ (AmqpRejectAndDontRequeueException)
curl -X POST localhost:8080/api/orders -H "content-type: application/json"   -d '{"orderId":"o-err1","customerId":"c-1","amount":-1}'
# blacklisted -> NonRetriableBusinessException -> DLQ
curl -X POST localhost:8080/api/orders -H "content-type: application/json"   -d '{"orderId":"o-err2","customerId":"c-bad","amount":10}'
```

## Skripte
```bash
infra/scripts/list.sh
infra/scripts/purge.sh q.order.created.dlq
infra/scripts/delete.sh q.order.created
# rabbitmqadmin (HTTP-API) herunterladen:
curl -u demo:demo http://localhost:15672/cli/rabbitmqadmin -o infra/scripts/rabbitmqadmin && chmod +x infra/scripts/rabbitmqadmin
# DLQ -> Replay
infra/scripts/dlq-republish.sh
```

> Hinweis: Wrapper-Dateien (`gradlew`, `gradle/wrapper/*`) sind nicht enthalten. Falls du den Wrapper verwenden willst:
> ```bash
> gradle wrapper --gradle-version 8.10.2 --distribution-type bin
> ./gradlew build
> ```
