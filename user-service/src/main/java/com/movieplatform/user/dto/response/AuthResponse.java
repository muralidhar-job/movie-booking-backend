package com.movieplatform.user.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class AuthResponse {
    private String token;
    private long expiresIn;
    private String role;
    private UUID userId;
    private String message;
}
