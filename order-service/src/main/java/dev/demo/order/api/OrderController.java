package dev.demo.order.api;

import dev.demo.dto.OrderCreated;
import dev.demo.dto.PaymentRequest;
import dev.demo.dto.PaymentResult;
import dev.demo.order.amqp.AmqpConfig;
import dev.demo.order.messaging.OrderPublisher;
import dev.demo.order.rpc.BillingRpcClient;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
  private static final Logger log = LoggerFactory.getLogger(OrderController.class);
  private static final String MDC_ORDER_ID = "orderId";

  public OrderController(RabbitTemplate rabbitTemplate, OrderPublisher publisher, BillingRpcClient billingRpcClient) {
    this.rabbitTemplate = rabbitTemplate;
    this.publisher = publisher;
    this.billingRpcClient = billingRpcClient;
  }

  @PostMapping
  public ResponseEntity<String> create(@RequestBody OrderCreated dto) {
  MDC.put(MDC_ORDER_ID, dto.orderId());
    log.info("Received create order request customerId={} amount={}", dto.customerId(), dto.amount());
    rabbitTemplate.convertAndSend(
        AmqpConfig.EXCHANGE_ORDERS,
        AmqpConfig.RK_ORDER_CREATED,
        dto,
        msg -> {
          msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          msg.getMessageProperties().setMessageId(UUID.randomUUID().toString());
          log.debug("Publishing order event routingKey={} messageId={}", AmqpConfig.RK_ORDER_CREATED, msg.getMessageProperties().getMessageId());
          return msg;
        }
    );
    log.info("Order event published orderId={}", dto.orderId());
    MDC.clear();
    return ResponseEntity.ok(dto.orderId());
  }

  @PostMapping("/async")
  public CompletableFuture<ResponseEntity<String>> createAsync(@RequestBody OrderCreated dto) {
  MDC.put(MDC_ORDER_ID, dto.orderId());
    log.info("Received async create order request customerId={} amount={}", dto.customerId(), dto.amount());
    return publisher.publishAsync(dto)
        .thenApply(ack -> ack
            ? ResponseEntity.ok("Broker ACK for " + dto.orderId())
            : ResponseEntity.status(502).body("Broker NACK for " + dto.orderId()))
        .whenComplete((res, ex) -> {
          if (ex != null) {
            log.error("Async publish failed orderId={}", dto.orderId(), ex);
          } else {
            log.info("Async publish completed orderId={} status={}", dto.orderId(), res.getStatusCode());
          }
          MDC.clear();
        });
  }

  @PostMapping("/billing/check-async")
  public CompletableFuture<PaymentResult> checkBilling(@RequestBody PaymentRequest req) {
  MDC.put(MDC_ORDER_ID, req.orderId());
    log.info("Billing check async started amount={} customerId={}", req.amount(), req.customerId());
    return billingRpcClient.checkAsync(req);
  }
}
