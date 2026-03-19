package com.premiersport.order.controller;

import com.premiersport.common.dto.ApiResponse;
import com.premiersport.order.service.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class PaymentController {

    private final StripeService stripeService;

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentIntent(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        Map<String, String> result = stripeService.createPaymentIntent(id, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        stripeService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("ok");
    }
}
