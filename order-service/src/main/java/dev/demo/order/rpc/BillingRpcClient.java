package dev.demo.order.rpc;

import dev.demo.dto.PaymentRequest;
import dev.demo.dto.PaymentResult;
import dev.demo.order.amqp.AmqpConfig;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class BillingRpcClient {

  private final AsyncRabbitTemplate asyncTpl;

  public BillingRpcClient(AsyncRabbitTemplate asyncTpl) {
    this.asyncTpl = asyncTpl;
  }

  public CompletableFuture<PaymentResult> checkAsync(PaymentRequest req) {
    var type = new ParameterizedTypeReference<PaymentResult>() {};
    return asyncTpl.convertSendAndReceiveAsType(
        AmqpConfig.EXCHANGE_ORDERS,
        AmqpConfig.RK_BILLING_CHECK,
        req,
        type
    );
  }
}
