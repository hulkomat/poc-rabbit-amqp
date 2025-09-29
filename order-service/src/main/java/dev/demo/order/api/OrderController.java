package dev.demo.order.api;

import dev.demo.dto.OrderCreated;
import dev.demo.dto.PaymentRequest;
import dev.demo.dto.PaymentResult;
import dev.demo.order.amqp.AmqpConfig;
import dev.demo.order.messaging.OrderPublisher;
import dev.demo.order.rpc.BillingRpcClient;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final RabbitTemplate rabbitTemplate;
  private final OrderPublisher publisher;
  private final BillingRpcClient billingRpcClient;

  public OrderController(RabbitTemplate rabbitTemplate, OrderPublisher publisher, BillingRpcClient billingRpcClient) {
    this.rabbitTemplate = rabbitTemplate;
    this.publisher = publisher;
    this.billingRpcClient = billingRpcClient;
  }

  @PostMapping
  public ResponseEntity<String> create(@RequestBody OrderCreated dto) {
    rabbitTemplate.convertAndSend(
        AmqpConfig.EXCHANGE_ORDERS,
        AmqpConfig.RK_ORDER_CREATED,
        dto,
        msg -> {
          msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          msg.getMessageProperties().setMessageId(UUID.randomUUID().toString());
          return msg;
        }
    );
    return ResponseEntity.ok(dto.orderId());
  }

  @PostMapping("/async")
  public CompletableFuture<ResponseEntity<String>> createAsync(@RequestBody OrderCreated dto) {
    return publisher.publishAsync(dto)
        .thenApply(ack -> ack
            ? ResponseEntity.ok("Broker ACK for " + dto.orderId())
            : ResponseEntity.status(502).body("Broker NACK for " + dto.orderId()));
  }

  @PostMapping("/billing/check-async")
  public CompletableFuture<PaymentResult> checkBilling(@RequestBody PaymentRequest req) {
    return billingRpcClient.checkAsync(req);
  }
}
