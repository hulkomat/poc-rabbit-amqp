package dev.demo.billing.consumer;

import dev.demo.billing.amqp.AmqpConfig;
import dev.demo.dto.OrderCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BillingListener {
  private static final Logger log = LoggerFactory.getLogger(BillingListener.class);
  private int counter = 0;

  @RabbitListener(queues = AmqpConfig.QUEUE_ORDER_CREATED)
  public void onOrderCreated(OrderCreated evt) {
    if (evt.amount() < 0) {
      throw new AmqpRejectAndDontRequeueException("Negative amount is not allowed");
    }
    if ("c-bad".equals(evt.customerId())) {
      throw new AmqpConfig.NonRetriableBusinessException("Blacklisted customer");
    }
    if ((counter++ % 3) != 2) {
      throw new IllegalStateException("Transient billing failure, try again");
    }
    log.info("✅ Billing processed orderId={} amount={}", evt.orderId(), evt.amount());
  }
}
