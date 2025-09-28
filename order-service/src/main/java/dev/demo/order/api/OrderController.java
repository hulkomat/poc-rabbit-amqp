package dev.demo.order.api;

import dev.demo.dto.OrderCreated;
import dev.demo.order.amqp.AmqpConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final RabbitTemplate rabbitTemplate;

  public OrderController(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
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
}
