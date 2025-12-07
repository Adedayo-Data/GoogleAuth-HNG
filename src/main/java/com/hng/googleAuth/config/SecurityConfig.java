package com.hng.googleAuth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/payments/paystack/webhook",
                                                                "/payments/paystack/initiate") // Disable CSRF for
                                                                                               // webhook and payment
                                )
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Allow
                                                                                                          // sessions
                                                                                                          // for OAuth
                                )
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/auth/**", "/oauth2/**", "/login/**",
                                                                "/payments/paystack/webhook",
                                                                "/payments/{reference}/status")
                                                .permitAll()
                                                .requestMatchers("/users/me", "/payments/my-transactions",
                                                                "/payments/paystack/initiate")
                                                .authenticated()
                                                .anyRequest().permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .defaultSuccessUrl("/auth/google/callback", true))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
