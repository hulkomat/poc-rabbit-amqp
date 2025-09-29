package dev.demo.dto;
public record PaymentRequest(String orderId, String customerId, double amount) {}
