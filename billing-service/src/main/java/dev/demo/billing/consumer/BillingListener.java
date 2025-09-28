package dev.demo.billing.consumer;

import dev.demo.dto.OrderCreated;
import dev.demo.billing.amqp.AmqpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BillingListener {
  private static final Logger log = LoggerFactory.getLogger(BillingListener.class);

  @RabbitListener(queues = AmqpConfig.QUEUE_ORDER_CREATED)
  public void onOrderCreated(OrderCreated evt) {
    log.info("💳 Billing received orderId={} customerId={} amount={}",
        evt.orderId(), evt.customerId(), evt.amount());
  }
}
