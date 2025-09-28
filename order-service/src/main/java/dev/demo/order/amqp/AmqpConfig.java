package dev.demo.order.amqp;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

  public static final String EXCHANGE_ORDERS = "ex.orders";
  public static final String RK_ORDER_CREATED = "order.created";

  @Bean
  public CachingConnectionFactory connectionFactory() {
    var cf = new CachingConnectionFactory("localhost", 5672);
    cf.setUsername("demo");
    cf.setPassword("demo");
    // Für spätere Lektionen (Publisher Confirms):
    cf.setPublisherConfirmType(ConfirmType.CORRELATED);
    cf.setPublisherReturns(true);
    return cf;
  }

  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(CachingConnectionFactory cf) {
    var tpl = new RabbitTemplate(cf);
    tpl.setMessageConverter(messageConverter());
    tpl.setMandatory(true); // ReturnCallback bei unroutable Messages
    return tpl;
  }

  // Producer deklariert typischerweise Exchanges (nicht zwingend Queues)
  @Bean
  public TopicExchange ordersExchange() {
    return new TopicExchange(EXCHANGE_ORDERS, true, false);
  }

  // Führt Deklarationen aus (Exchange); ungefährlich, wenn schon vorhanden.
  @Bean
  public RabbitAdmin amqpAdmin(CachingConnectionFactory cf) {
    return new RabbitAdmin(cf);
  }
}
