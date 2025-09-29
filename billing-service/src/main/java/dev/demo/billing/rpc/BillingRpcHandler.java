package dev.demo.billing.rpc;

import dev.demo.billing.amqp.AmqpConfig;
import dev.demo.dto.PaymentRequest;
import dev.demo.dto.PaymentResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BillingRpcHandler {

  @RabbitListener(queues = AmqpConfig.QUEUE_BILLING_RPC)
  public PaymentResult check(PaymentRequest req) {
    boolean ok = req.amount() >= 0 && !"c-bad".equals(req.customerId());
    String reason = ok ? "OK" : (req.amount() < 0 ? "NEGATIVE_AMOUNT" : "BLACKLISTED");
    return new PaymentResult(req.orderId(), ok, reason);
  }
}
