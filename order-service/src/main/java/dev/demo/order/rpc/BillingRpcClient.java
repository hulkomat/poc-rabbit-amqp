package dev.demo.order.rpc;

import dev.demo.dto.PaymentRequest;
import dev.demo.dto.PaymentResult;
import dev.demo.order.amqp.AmqpConfig;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;

@Component
public class BillingRpcClient {

  private final AsyncRabbitTemplate asyncTpl;
  private static final Logger log = LoggerFactory.getLogger(BillingRpcClient.class);
  private static final String MDC_ORDER_ID = "orderId";

  public BillingRpcClient(AsyncRabbitTemplate asyncTpl) {
    this.asyncTpl = asyncTpl;
  }

  public CompletableFuture<PaymentResult> checkAsync(PaymentRequest req) {
    MDC.put(MDC_ORDER_ID, req.orderId());
    log.info("Sending billing RPC orderId={} amount={} customerId={}", req.orderId(), req.amount(), req.customerId());
    var type = new ParameterizedTypeReference<PaymentResult>() {};
    return asyncTpl.convertSendAndReceiveAsType(
            AmqpConfig.EXCHANGE_ORDERS,
            AmqpConfig.RK_BILLING_CHECK,
            req,
            type
        )
        .whenComplete((res, ex) -> {
          if (ex != null) {
            log.error("Billing RPC failed orderId={}", req.orderId(), ex);
          } else if (res != null) {
            log.info("Billing RPC response orderId={} success={} reason={}", res.orderId(), res.success(), res.reason());
          } else {
            log.warn("Billing RPC returned null response orderId={}", req.orderId());
          }
          MDC.clear();
        });
  }
}
