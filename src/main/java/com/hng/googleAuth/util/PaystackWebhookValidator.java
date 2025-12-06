package com.hng.googleAuth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
@Slf4j
public class PaystackWebhookValidator {

    public boolean validateSignature(String payload, String signature, String secret) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(keySpec);

            byte[] macData = sha512Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(macData);

            return computedSignature.equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
