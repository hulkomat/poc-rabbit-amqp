package dev.demo.dto;
public record PaymentResult(String orderId, boolean approved, String reason) {}
