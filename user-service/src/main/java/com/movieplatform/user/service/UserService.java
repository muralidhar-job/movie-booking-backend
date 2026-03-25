package com.movieplatform.user.service;

import com.movieplatform.user.dto.request.LoginRequest;
import com.movieplatform.user.dto.request.RegisterRequest;
import com.movieplatform.user.dto.response.AuthResponse;
import com.movieplatform.user.entity.User;
import com.movieplatform.user.exception.DuplicateEmailException;
import com.movieplatform.user.exception.InvalidCredentialsException;
import com.movieplatform.user.repository.UserRepository;
import com.movieplatform.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User service — registration, authentication, JWT issuance.
 * Design Patterns: Service Layer, Builder (DTOs).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .city(request.getCity())
            .role(User.Role.valueOf(request.getRole().toUpperCase()))
            .build();

        User saved = userRepository.save(user);
        log.info("New user registered: id={} email={} role={}", saved.getId(), saved.getEmail(), saved.getRole());

        String token = jwtUtil.generateToken(
            saved.getId().toString(), saved.getEmail(), saved.getRole().name());

        return AuthResponse.builder()
            .userId(saved.getId())
            .token(token)
            .expiresIn(jwtUtil.getExpirationMs() / 1000)
            .role(saved.getRole().name())
            .message("Registration successful")
            .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User login: id={} role={}", user.getId(), user.getRole());

        String token = jwtUtil.generateToken(
            user.getId().toString(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
            .userId(user.getId())
            .token(token)
            .expiresIn(jwtUtil.getExpirationMs() / 1000)
            .role(user.getRole().name())
            .message("Login successful")
            .build();
    }

    @Transactional(readOnly = true)
    public User getProfile(String userId) {
        return userRepository.findById(java.util.UUID.fromString(userId))
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
