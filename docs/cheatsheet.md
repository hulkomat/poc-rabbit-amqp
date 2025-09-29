# RabbitMQ Cheatsheet (für dieses Demo-Projekt)

## Ports & Zugang
- AMQP: **5672**
- Management UI: **15672** (`demo/demo`)

## Exchanges/Queues (in diesem Projekt)
- Exchange: `ex.orders` (topic)
- Routing Keys: `order.created`, `billing.check`
- Queues (Consumer-owned):
  - `q.order.created` (bindet `order.created`)
  - `q.order.created.dlq` (DLQ via `ex.dlx` + RK `order.created.dlq`)
  - `q.billing.rpc` (bindet `billing.check`)

## Wichtige Spring AMQP Einstellungen
- **Publisher Confirms**: `CachingConnectionFactory#setPublisherConfirmType(CORRELATED)`
- **Returns (unroutable)**: `RabbitTemplate#setMandatory(true)` + `setReturnsCallback(...)`
- **JSON Converter**: `Jackson2JsonMessageConverter` (Producer & Consumer)
- **Listener Retry**: `RetryInterceptorBuilder.stateless()` + `RetryTemplate` (Backoff, MaxAttempts)
- **DLQ**: an der **Queue** per args `x-dead-letter-exchange`, `x-dead-letter-routing-key`

## DLQ Analyse
- Header **`x-death`** enthält: `count`, `reason`, `exchange`, `routing-keys`, `time`.

## CLI/HTTP-API
- In Container:
  - `rabbitmqctl list_queues name messages arguments`
  - `rabbitmqctl purge_queue <queue>` / `delete_queue <queue>`
- HTTP-API (`rabbitmqadmin`):
  - `rabbitmqadmin list queues`
  - `rabbitmqadmin get queue=q.order.created.dlq ackmode=ack_requeue_false requeue=false count=1`
  - `rabbitmqadmin publish exchange=ex.orders routing_key=order.created payload='{}' properties='{"content_type":"application/json"}'`

## RPC (Request/Reply)
- Client: `AsyncRabbitTemplate.convertSendAndReceiveAsType(...)` ⇒ **CompletableFuture**
- Mechanismus: **Direct-Reply-To** (`amq.rabbitmq.reply-to`), keine eigene Reply-Queue nötig.
