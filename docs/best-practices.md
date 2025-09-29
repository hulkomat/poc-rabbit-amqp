# Best Practices (RabbitMQ + Spring)

## Architektur & Ownership
- Producer deklariert **Exchanges**, Consumer deklariert **Queues + Bindings** (Ownership & Entkopplung).
- Plane **At-Least-Once**: Consumer idempotent (Dedup nach `messageId`/`correlationId`).

## Zuverlässigkeit
- Durable + persistente Nachrichten (`delivery_mode=2`).
- Publisher Confirms + Returns aktivieren (unroutable erkennen).
- Listener-Retry mit Backoff + DLQ statt blindem Requeue.

## Tuning
- **Prefetch** passend (z. B. 20–100), **Concurrency** graduell erhöhen.
- **Lazy queues** (`x-queue-mode=lazy`) für große Backlogs.
- **TTL/Max-Length** nutzen (`x-message-ttl`, `x-max-length`, `overflow=reject-publish-dlx`).

## Observability
- CorrelationId/MessageId in Logs (MDC), Metriken (Micrometer).
- DLQ überwachen (`x-death`, DLQ-Rate).

## Sicherheit
- VHosts, minimal nötige Rechte, TLS (Port 5671), Management-UI absichern.
