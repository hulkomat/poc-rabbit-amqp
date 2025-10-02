package dev.demo.order.messaging;

import dev.demo.dto.OrderCreated;
import dev.demo.order.amqp.AmqpConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class OrderPublisher {

  private final RabbitTemplate rabbit;
  private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);
  private static final String MDC_ORDER_ID = "orderId";

  public OrderPublisher(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
    this.rabbit.setReturnsCallback(returned -> log.warn("Unroutable message reply={} exchange={} rk={} correlation={}",
        returned.getReplyText(), returned.getExchange(), returned.getRoutingKey(), returned.getCorrelationId()));
  }

  public CompletableFuture<Boolean> publishAsync(OrderCreated dto) {
  MDC.put(MDC_ORDER_ID, dto.orderId());
  var corr = new CorrelationData(UUID.randomUUID().toString());
  CompletableFuture<Boolean> confirm = corr.getFuture().thenApply(correlation -> correlation.isAck());
  log.info("Publishing order async orderId={} correlationDataId={}", dto.orderId(), corr.getId());

    rabbit.convertAndSend(
        AmqpConfig.EXCHANGE_ORDERS,
        AmqpConfig.RK_ORDER_CREATED,
        dto,
        msg -> {
          msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          msg.getMessageProperties().setMessageId(UUID.randomUUID().toString());
          msg.getMessageProperties().setCorrelationId(dto.orderId());
          return msg;
        },
        corr
    );
    return confirm.whenComplete((ack, ex) -> {
      if (ex != null) {
        log.error("Broker confirm future failed orderId={}", dto.orderId(), ex);
      } else {
        log.info("Broker confirm received orderId={} ack={}", dto.orderId(), ack);
      }
      MDC.clear();
    });
  }
}
