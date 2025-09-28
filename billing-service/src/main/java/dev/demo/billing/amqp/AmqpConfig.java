package dev.demo.billing.amqp;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
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

  // DLX/DLQ-Namen
  public static final String DLX_NAME = "ex.dlx";
  public static final String DLQ_NAME = "q.order.created.dlq";
  public static final String DLQ_RK   = "order.created.dlq";

  @Bean
  public CachingConnectionFactory connectionFactory() {
    var cf = new CachingConnectionFactory("localhost", 5672);
    cf.setUsername("demo");
    cf.setPassword("demo");
    return cf;
  }

  // Producer-Exchange spiegeln (deklarieren ist idempotent)
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

  // Queue an Exchange binden
  @Bean
  public Binding bindOrderCreated(Queue orderCreatedQueue, TopicExchange ordersExchange) {
    return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(RK_ORDER_CREATED);
  }

  @Bean Jackson2JsonMessageConverter converter() { return new Jackson2JsonMessageConverter(); }

  // Retry-Template: definiert, welche Exceptions retriable sind
  @Bean
  RetryTemplate retryTemplate() {
    // Map: Exception-Klasse -> retryable?
    var retryables = Map.<Class<? extends Throwable>, Boolean>of(
        AmqpRejectAndDontRequeueException.class, false, // nie retrien (geht sofort DLQ)
        NonRetriableBusinessException.class, false      // unsere eigene "hart" Exception
    );
    var policy = new SimpleRetryPolicy(3, retryables, true); // max 3 Versuche; alle anderen true
    var template = new RetryTemplate();
    template.setRetryPolicy(policy);
    // Backoff: 500ms -> 1000ms -> 2000ms (einfaches, schnelles Beispiel)
    template.setBackOffPolicy(new org.springframework.retry.backoff.ExponentialBackOffPolicy() {{
      setInitialInterval(500);
      setMultiplier(2.0);
      setMaxInterval(5000);
    }});
    return template;
  }

  // Listener-Container mit Retry-Interceptor + DLQ-Recoverer
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      CachingConnectionFactory cf,
      RetryTemplate retryTemplate
  ) {
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(converter());
    f.setDefaultRequeueRejected(false); // wichtig: nack ohne Requeue -> DLX greift

    var interceptor = RetryInterceptorBuilder
        .stateless()
        .retryOperations(retryTemplate)
        .recoverer((msg, cause) -> {
          // Wenn Retries ausgereizt: Broker routet wegen requeue=false zur DLQ (über DLX).
          // Hier könnten wir zusätzlich loggen/telemetrieren.
          System.err.println("Retries exhausted; message will go to DLQ. cause=" + cause);
        })
        .build();

    f.setAdviceChain(interceptor);
    return f;
  }

  // Eigene "nicht retriable" Exception
  public static class NonRetriableBusinessException extends RuntimeException {
    public NonRetriableBusinessException(String msg) { super(msg); }
  }
}
