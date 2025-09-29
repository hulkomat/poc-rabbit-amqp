package dev.demo.billing.amqp;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class AmqpConfig {

  public static final String EXCHANGE_ORDERS = "ex.orders";
  public static final String RK_ORDER_CREATED = "order.created";
  public static final String QUEUE_ORDER_CREATED = "q.order.created";

  // DLX/DLQ names
  public static final String DLX_NAME = "ex.dlx";
  public static final String DLQ_NAME = "q.order.created.dlq";
  public static final String DLQ_RK   = "order.created.dlq";

  // RPC (billing.check)
  public static final String RK_BILLING_CHECK   = "billing.check";
  public static final String QUEUE_BILLING_RPC  = "q.billing.rpc";

  @Bean
  public CachingConnectionFactory connectionFactory() {
    var cf = new CachingConnectionFactory("localhost", 5672);
    cf.setUsername("demo");
    cf.setPassword("demo");
    return cf;
  }

  @Bean TopicExchange ordersExchange() { return new TopicExchange(EXCHANGE_ORDERS, true, false); }

  // DLX + DLQ
  @Bean DirectExchange dlx() { return new DirectExchange(DLX_NAME, true, false); }
  @Bean Queue orderCreatedDlq() { return QueueBuilder.durable(DLQ_NAME).build(); }
  @Bean Binding bindDlq() { return BindingBuilder.bind(orderCreatedDlq()).to(dlx()).with(DLQ_RK); }

  // Hauptqueue mit DLX-Verknüpfung
  @Bean
  public Queue orderCreatedQueue() {
    return QueueBuilder.durable(QUEUE_ORDER_CREATED)
        .withArgument("x-dead-letter-exchange", DLX_NAME)
        .withArgument("x-dead-letter-routing-key", DLQ_RK)
        .build();
  }

  // Binding für Events
  @Bean
  public Binding bindOrderCreated(Queue orderCreatedQueue, TopicExchange ordersExchange) {
    return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(RK_ORDER_CREATED);
  }

  // RPC Queue + Binding
  @Bean
  public Queue billingRpcQueue() { return QueueBuilder.durable(QUEUE_BILLING_RPC).build(); }

  @Bean
  public Binding bindBillingRpc(Queue billingRpcQueue, TopicExchange ordersExchange) {
    return BindingBuilder.bind(billingRpcQueue).to(ordersExchange).with(RK_BILLING_CHECK);
  }

  @Bean
  public Jackson2JsonMessageConverter converter() { return new Jackson2JsonMessageConverter(); }

  @Bean
  RetryTemplate retryTemplate() {
    var retryables = Map.<Class<? extends Throwable>, Boolean>of(
        AmqpRejectAndDontRequeueException.class, false,
        NonRetriableBusinessException.class, false
    );
    var policy = new SimpleRetryPolicy(3, retryables, true);
    var template = new RetryTemplate();
    var backoff = new org.springframework.retry.backoff.ExponentialBackOffPolicy();
    backoff.setInitialInterval(500);
    backoff.setMultiplier(2.0);
    backoff.setMaxInterval(5000);
    template.setRetryPolicy(policy);
    template.setBackOffPolicy(backoff);
    return template;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      CachingConnectionFactory cf,
      RetryTemplate retryTemplate
  ) {
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(converter());
    f.setDefaultRequeueRejected(false);
    f.setPrefetchCount(20);
    var interceptor = RetryInterceptorBuilder
        .stateless()
        .retryOperations(retryTemplate)
        .recoverer((msg, cause) -> System.err.println("Retries exhausted; to DLQ. cause=" + cause))
        .build();
    f.setAdviceChain(interceptor);
    return f;
  }

  @Bean
  public AmqpAdmin amqpAdmin(CachingConnectionFactory cf) { return new RabbitAdmin(cf); }

  // Custom non-retriable exception
  public static class NonRetriableBusinessException extends RuntimeException {
    public NonRetriableBusinessException(String msg) { super(msg); }
  }
}
