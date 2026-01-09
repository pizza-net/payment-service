package com.example.paymentservice.service;

import com.example.paymentservice.dto.CheckoutSessionResponse;
import com.example.paymentservice.dto.CreateCheckoutSessionRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Transactional
    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        log.info("Creating Stripe checkout session for order: {}", request.getOrderId());

        // Tworzenie sesji płatności w Stripe
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment-cancel?session_id={CHECKOUT_SESSION_ID}")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(request.getCurrency())
                                                .setUnitAmount(request.getAmount().multiply(new BigDecimal("100")).longValue()) // Stripe używa najmniejszej jednostki waluty (grosze)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Pizza Order #" + request.getOrderId())
                                                                .setDescription("Payment for pizza order")
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata("orderId", request.getOrderId().toString())
                .build();

        Session session = Session.create(params);

        // Zapisanie płatności w bazie danych
        Payment payment = new Payment();
        payment.setStripeSessionId(session.getId());
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);

        log.info("Stripe checkout session created: {}", session.getId());

        return new CheckoutSessionResponse(session.getId(), session.getUrl());
    }

    @Transactional
    public PaymentResponse handleSuccessfulPayment(String sessionId) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        log.info("Handling successful payment for session: {}", sessionId);

        // Pobierz sesję z Stripe
        Session session = Session.retrieve(sessionId);

        // Znajdź płatność w bazie danych
        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for session: " + sessionId));

        // Zaktualizuj status płatności
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setStripePaymentIntentId(session.getPaymentIntent());
        paymentRepository.save(payment);

        // Powiadom order-service o udanej płatności
        try {
            notifyOrderService(payment.getOrderId(), "PAID");
        } catch (Exception e) {
            log.error("Failed to notify order-service", e);
        }

        log.info("Payment completed successfully for order: {}", payment.getOrderId());

        return mapToPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse handleCancelledPayment(String sessionId) {
        log.info("Handling cancelled payment for session: {}", sessionId);

        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for session: " + sessionId));

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        // Powiadom order-service o anulowanej płatności
        try {
            notifyOrderService(payment.getOrderId(), "CANCELLED");
        } catch (Exception e) {
            log.error("Failed to notify order-service", e);
        }

        log.info("Payment cancelled for order: {}", payment.getOrderId());

        return mapToPaymentResponse(payment);
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        return mapToPaymentResponse(payment);
    }

    private void notifyOrderService(Long orderId, String status) {
        webClientBuilder.build()
                .patch()
                .uri(orderServiceUrl + "/api/orders/" + orderId + "/payment-status")
                .bodyValue(status)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getStripeSessionId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getStripePaymentIntentId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}

