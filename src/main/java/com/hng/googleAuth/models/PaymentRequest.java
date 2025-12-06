package com.hng.googleAuth.models;

import lombok.Data;

@Data
public class PaymentRequest {
    private String email; // customer email (required by Paystack)
    private Long amount; // amount in kobo
}
