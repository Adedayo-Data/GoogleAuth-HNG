package com.hng.googleAuth.service;

import com.hng.googleAuth.dto.UserResponseDTO;
import com.hng.googleAuth.models.Users;
import com.hng.googleAuth.repository.UserRepository;
import com.hng.googleAuth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public Map<String, Object> createOrUpdateUser(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        String googleId = oauth2User.getAttribute("sub");

        if (email == null || googleId == null) {
            throw new IllegalArgumentException("Missing required user information from Google");
        }

        Users user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    Users newUser = Users.builder()
                            .email(email)
                            .name(name != null ? name : "Unknown")
                            .picture(picture)
                            .googleId(googleId)
                            .build();
                    return userRepository.save(newUser);
                });

        if (!user.getEmail().equals(email) ||
                !user.getName().equals(name) ||
                (picture != null && !picture.equals(user.getPicture()))) {
            user.setEmail(email);
            user.setName(name != null ? name : user.getName());
            user.setPicture(picture);
            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        UserResponseDTO userResponse = UserResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .picture(user.getPicture())
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("user", userResponse);
        response.put("token", token);
        response.put("message", "Authentication successful");

        return response;
    }
}
