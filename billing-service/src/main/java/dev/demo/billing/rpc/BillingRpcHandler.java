package dev.demo.billing.rpc;

import dev.demo.billing.amqp.AmqpConfig;
import dev.demo.dto.PaymentRequest;
import dev.demo.dto.PaymentResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class BillingRpcHandler {

  private static final Logger log = LoggerFactory.getLogger(BillingRpcHandler.class);
  private static final String MDC_ORDER_ID = "orderId";

  @RabbitListener(queues = AmqpConfig.QUEUE_BILLING_RPC)
  public PaymentResult check(PaymentRequest req) {
    MDC.put(MDC_ORDER_ID, req.orderId());
    log.info("Billing RPC request orderId={} amount={} customerId={}", req.orderId(), req.amount(), req.customerId());
    boolean ok = req.amount() >= 0 && !"c-bad".equals(req.customerId());
    String reason;
    if (ok) {
      reason = "OK";
    } else if (req.amount() < 0) {
      reason = "NEGATIVE_AMOUNT";
    } else {
      reason = "BLACKLISTED";
    }
    log.info("Billing RPC result orderId={} success={} reason={}", req.orderId(), ok, reason);
    MDC.clear();
    return new PaymentResult(req.orderId(), ok, reason);
  }
}
