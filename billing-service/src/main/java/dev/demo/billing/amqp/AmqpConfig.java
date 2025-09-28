package dev.demo.billing.amqp;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AmqpConfig {

  public static final String EXCHANGE_ORDERS = "ex.orders";
  public static final String RK_ORDER_CREATED = "order.created";
  public static final String QUEUE_ORDER_CREATED = "q.order.created";

  @Bean
  public CachingConnectionFactory connectionFactory() {
    var cf = new CachingConnectionFactory("localhost", 5672);
    cf.setUsername("demo");
    cf.setPassword("demo");
    return cf;
  }

  @Bean
  public TopicExchange ordersExchange() {
    return new TopicExchange(EXCHANGE_ORDERS, true, false);
  }

  // Consumer deklariert seine Queue
  @Bean
  public Queue orderCreatedQueue() {
    return QueueBuilder.durable(QUEUE_ORDER_CREATED).build();
  }

  @Bean
  public Binding bindOrderCreated(Queue orderCreatedQueue, TopicExchange ordersExchange) {
    return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(RK_ORDER_CREATED);
  }

  @Bean
  public Jackson2JsonMessageConverter converter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(CachingConnectionFactory cf) {
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(converter());
    // wichtig für spätere DLQ-Lektion: Exceptions -> nack (ohne Requeue)
    f.setDefaultRequeueRejected(false);
    return f;
  }

  @Bean
  public AmqpAdmin amqpAdmin(CachingConnectionFactory cf) { return new RabbitAdmin(cf); }
}
