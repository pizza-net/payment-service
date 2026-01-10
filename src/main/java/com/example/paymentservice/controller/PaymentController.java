package com.example.paymentservice.controller;

import com.example.paymentservice.dto.CheckoutSessionResponse;
import com.example.paymentservice.dto.CreateCheckoutSessionRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @RequestBody CreateCheckoutSessionRequest request) {
        try {
            log.info("Received request to create checkout session for order: {}", request.getOrderId());
            CheckoutSessionResponse response = paymentService.createCheckoutSession(request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error while creating checkout session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error creating checkout session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/success")
    public ResponseEntity<PaymentResponse> handleSuccessfulPayment(
            @RequestParam("session_id") String sessionId) {
        try {
            log.info("Handling successful payment for session: {}", sessionId);
            PaymentResponse response = paymentService.handleSuccessfulPayment(sessionId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error while handling successful payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error handling successful payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<PaymentResponse> handleCancelledPayment(
            @RequestParam("session_id") String sessionId) {
        try {
            log.info("Handling cancelled payment for session: {}", sessionId);
            PaymentResponse response = paymentService.handleCancelledPayment(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error handling cancelled payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        try {
            log.info("Getting payment for order: {}", orderId);
            PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting payment by order ID", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/verify-session")
    public ResponseEntity<PaymentResponse> verifySession(@RequestBody VerifySessionRequest request) {
        try {
            log.info("Verifying session: {}", request.getSessionId());
            PaymentResponse response = paymentService.handleSuccessfulPayment(request.getSessionId());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error while verifying session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error verifying session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DTO for verify-session request
    public static class VerifySessionRequest {
        private String sessionId;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}

