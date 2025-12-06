package com.hng.googleAuth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hng.googleAuth.dto.PaymentInitiateResponseDTO;
import com.hng.googleAuth.dto.TransactionStatusResponseDTO;
import com.hng.googleAuth.models.PaymentRequest;
import com.hng.googleAuth.service.PaystackService;
import com.hng.googleAuth.util.PaystackWebhookValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaystackService paystackService;
    private final PaystackWebhookValidator webhookValidator;
    private final ObjectMapper objectMapper;

    @Value("${paystack.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/paystack/initiate")
    public ResponseEntity<PaymentInitiateResponseDTO> initiatePayment(@RequestBody PaymentRequest request) {
        PaymentInitiateResponseDTO response = paystackService.initializeTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/paystack/webhook")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {

        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook received without signature");
            throw new IllegalArgumentException("Missing webhook signature");
        }

        // Validate signature
        if (!webhookValidator.validateSignature(payload, signature, webhookSecret)) {
            log.warn("Invalid webhook signature");
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        try {
            // Parse webhook payload
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String event = (String) webhookData.get("event");
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");

            if (data == null) {
                throw new IllegalArgumentException("Invalid webhook payload");
            }

            String reference = (String) data.get("reference");
            String status = (String) data.get("status");

            log.info("Webhook received - Event: {}, Reference: {}, Status: {}", event, reference, status);

            // Update transaction status in database
            if ("charge.success".equals(event)) {
                paystackService.updateTransactionStatus(reference, "success");
            } else if ("charge.failed".equals(event)) {
                paystackService.updateTransactionStatus(reference, "failed");
            }

            return ResponseEntity.ok(Map.of("status", true));

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    @GetMapping("/{reference}/status")
    public ResponseEntity<TransactionStatusResponseDTO> getTransactionStatus(
            @PathVariable String reference,
            @RequestParam(defaultValue = "false") boolean refresh) {

        TransactionStatusResponseDTO response = paystackService.getTransactionStatus(reference, refresh);
        return ResponseEntity.ok(response);
    }
}
