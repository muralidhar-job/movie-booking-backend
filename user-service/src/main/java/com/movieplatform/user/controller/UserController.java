package com.movieplatform.user.controller;

import com.movieplatform.user.dto.request.LoginRequest;
import com.movieplatform.user.dto.request.RegisterRequest;
import com.movieplatform.user.dto.response.AuthResponse;
import com.movieplatform.user.entity.User;
import com.movieplatform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "User & Auth", description = "Registration, login, profile")
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/register")
    @Operation(summary = "Register new customer or theatre admin")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Authenticate and receive JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get current user profile (JWT required)")
    public ResponseEntity<User> getProfile(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}
