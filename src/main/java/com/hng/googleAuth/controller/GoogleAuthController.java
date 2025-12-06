package com.hng.googleAuth.controller;

import com.hng.googleAuth.dto.UserResponseDTO;
import com.hng.googleAuth.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @GetMapping("/google")
    public ResponseEntity<Map<String, String>> triggerGoogleAuth() {
        // This endpoint will trigger OAuth2 redirect via Spring Security
        // The actual redirect is handled by Spring Security OAuth2 Client
        return ResponseEntity.ok(Map.of(
                "message", "Redirecting to Google OAuth",
                "google_auth_url", "/oauth2/authorization/google"));
    }

    @GetMapping("/google/callback")
    public ResponseEntity<UserResponseDTO> handleGoogleCallback(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            throw new IllegalArgumentException("Missing authorization code");
        }

        UserResponseDTO userResponse = googleAuthService.createOrUpdateUser(oauth2User);
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }
}
