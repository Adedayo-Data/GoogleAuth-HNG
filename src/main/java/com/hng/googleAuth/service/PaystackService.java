package com.hng.googleAuth.service;

import com.hng.googleAuth.dto.PaymentInitiateResponseDTO;
import com.hng.googleAuth.dto.TransactionStatusResponseDTO;
import com.hng.googleAuth.exception.PaymentException;
import com.hng.googleAuth.models.PaymentRequest;
import com.hng.googleAuth.models.Transaction;
import com.hng.googleAuth.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackService {

    @Value("${paystack.secret-key}")
    private String secretKey;

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public PaymentInitiateResponseDTO initializeTransaction(PaymentRequest request, UUID userId, String userEmail) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("email", userEmail);
            Integer amountInKobo = (int) Math.round(request.getAmount() * 100);
            body.put("amount", amountInKobo);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = "https://api.paystack.co/transaction/initialize";
            log.info("Initializing Paystack transaction for user: {}, amount: {}", userId, request.getAmount());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            log.info("Paystack API response status: {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Paystack API returned non-OK status: {}", response.getStatusCode());
                throw new PaymentException("Failed to initialize payment with Paystack");
            }

            Map<String, Object> responseBody = response.getBody();
            Boolean status = (Boolean) responseBody.get("status");

            if (status == null || !status) {
                String message = (String) responseBody.get("message");
                log.error("Paystack API returned status=false. Message: {}", message);
                throw new PaymentException("Paystack error: " + (message != null ? message : "Unknown error"));
            }

            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

            if (data == null) {
                log.error("Paystack API response missing 'data' field");
                throw new PaymentException("Invalid response from Paystack");
            }

            String reference = (String) data.get("reference");
            String authorizationUrl = (String) data.get("authorization_url");

            // Check for duplicate transaction (idempotency)
            if (transactionRepository.existsByReference(reference)) {
                Transaction existingTransaction = transactionRepository.findByReference(reference)
                        .orElseThrow(() -> new PaymentException("Transaction reference conflict"));

                return PaymentInitiateResponseDTO.builder()
                        .reference(existingTransaction.getReference())
                        .authorizationUrl(existingTransaction.getAuthorizationUrl())
                        .build();
            }

            // Save transaction to database with userId
            Transaction transaction = Transaction.builder()
                    .reference(reference)
                    .amount(request.getAmount())
                    .status("pending")
                    .authorizationUrl(authorizationUrl)
                    .userId(userId) // Link to authenticated user
                    .build();

            transactionRepository.save(transaction);

            return PaymentInitiateResponseDTO.builder()
                    .reference(reference)
                    .authorizationUrl(authorizationUrl)
                    .build();

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // This catches timeout and connection errors
            log.error("Timeout or connection error calling Paystack API: {}", e.getMessage());
            throw new PaymentException(
                    "Unable to connect to Paystack. Please check your internet connection or try again later.", e);
        } catch (RestClientException e) {
            log.error("Error calling Paystack API", e);
            throw new PaymentException("Payment initiation failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void updateTransactionStatus(String reference, String status) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        transaction.setStatus(status);
        if ("success".equalsIgnoreCase(status)) {
            transaction.setPaidAt(LocalDateTime.now());
        }
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionStatusResponseDTO getTransactionStatus(String reference, boolean refresh) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // If refresh is requested, fetch live status from Paystack
        if (refresh) {
            Map<String, Object> verificationResult = verifyTransactionFromPaystack(reference);
            String liveStatus = (String) ((Map<?, ?>) verificationResult.get("data")).get("status");

            if (!transaction.getStatus().equals(liveStatus)) {
                transaction.setStatus(liveStatus);
                if ("success".equalsIgnoreCase(liveStatus)) {
                    transaction.setPaidAt(LocalDateTime.now());
                }
                transactionRepository.save(transaction);
            }
        }

        return TransactionStatusResponseDTO.builder()
                .reference(transaction.getReference())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .paidAt(transaction.getPaidAt())
                .build();
    }

    private Map<String, Object> verifyTransactionFromPaystack(String reference) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.paystack.co/transaction/verify/" + reference;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new PaymentException("Failed to verify transaction with Paystack");
            }

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error verifying transaction with Paystack", e);
            throw new PaymentException("Transaction verification failed", e);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<TransactionStatusResponseDTO> getUserTransactions(java.util.UUID userId) {
        java.util.List<Transaction> transactions = transactionRepository.findByUserId(userId);

        return transactions.stream()
                .map(transaction -> TransactionStatusResponseDTO.builder()
                        .reference(transaction.getReference())
                        .status(transaction.getStatus())
                        .amount(transaction.getAmount())
                        .paidAt(transaction.getPaidAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}
