package com.hng.googleAuth.models;

import lombok.Data;

@Data
public class PaymentRequest {
    private String email;
    private Double amount;
}
