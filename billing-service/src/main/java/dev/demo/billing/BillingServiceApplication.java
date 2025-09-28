package dev.demo.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableRabbit
public class BillingServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(BillingServiceApplication.class, args);
  }
}
