package dev.demo.order.messaging;

import dev.demo.dto.OrderCreated;
import dev.demo.order.amqp.AmqpConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class OrderPublisher {

  private final RabbitTemplate rabbit;

  public OrderPublisher(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
    this.rabbit.setReturnsCallback(returned -> {
      System.err.println("Unroutable message returned: reply=" + returned.getReplyText() +
          " exchange=" + returned.getExchange() + " rk=" + returned.getRoutingKey());
    });
  }

  public CompletableFuture<Boolean> publishAsync(OrderCreated dto) {
    var corr = new CorrelationData(UUID.randomUUID().toString());
    CompletableFuture<Boolean> confirm = corr.getFuture().thenApply(confirm -> confirm.isAck());

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
    return confirm;
  }
}
