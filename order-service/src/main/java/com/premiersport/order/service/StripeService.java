package com.premiersport.order.service;

import com.premiersport.common.exception.ApiException;
import com.premiersport.order.entity.OrderEntity;
import com.premiersport.order.repository.OrderRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    // Fix #9: Set Stripe.apiKey once at startup to avoid thread-unsafe repeated assignment
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe initialized");
    }

    public Map<String, String> createPaymentIntent(String orderId, String userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw ApiException.forbidden("Access denied");
        }

        if (order.getPaymentStatus() == OrderEntity.PaymentStatus.PAID) {
            throw ApiException.badRequest("Order is already paid");
        }

        try {
            long amountInPence = Math.round(order.getTotal() * 100);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPence)
                    .setCurrency("gbp")
                    .putMetadata("orderId", orderId)
                    .putMetadata("orderNumber", order.getOrderNumber())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            order.setStripePaymentIntentId(intent.getId());
            orderRepository.save(order);

            return Map.of("clientSecret", intent.getClientSecret());

        } catch (StripeException e) {
            log.error("Stripe error creating payment intent for order {}: {}", orderId, e.getMessage());
            throw ApiException.badRequest("Payment processing error: " + e.getMessage());
        }
    }

    public void handleWebhook(String payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank() || webhookSecret.equals("whsec_placeholder")) {
            log.warn("Stripe webhook secret not configured – webhook ignored");
            return;
        }

        if (sigHeader == null || sigHeader.isBlank()) {
            throw ApiException.badRequest("Missing Stripe-Signature header");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature");
            throw ApiException.badRequest("Invalid webhook signature");
        }

        processEvent(event);
    }

    private void processEvent(Event event) {
        log.info("Processing Stripe event: {}", event.getType());

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        switch (event.getType()) {
            case "payment_intent.succeeded" -> deserializer.getObject().ifPresent(obj -> {
                if (obj instanceof PaymentIntent intent) handlePaymentSucceeded(intent);
            });
            case "payment_intent.payment_failed" -> deserializer.getObject().ifPresent(obj -> {
                if (obj instanceof PaymentIntent intent) handlePaymentFailed(intent);
            });
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(PaymentIntent intent) {
        String orderId = intent.getMetadata().get("orderId");
        if (orderId == null) return;

        orderRepository.findById(orderId).ifPresent(order -> {
            order.setPaymentStatus(OrderEntity.PaymentStatus.PAID);
            order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Payment succeeded for order: {}", order.getOrderNumber());
            emailService.sendOrderConfirmation(order);
        });
    }

    private void handlePaymentFailed(PaymentIntent intent) {
        String orderId = intent.getMetadata().get("orderId");
        if (orderId == null) return;

        orderRepository.findById(orderId).ifPresent(order -> {
            order.setPaymentStatus(OrderEntity.PaymentStatus.UNPAID);
            orderRepository.save(order);
            log.warn("Payment failed for order: {}", order.getOrderNumber());
        });
    }
}
