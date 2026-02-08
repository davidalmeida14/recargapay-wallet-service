package br.com.recargapay.wallet.application.definitions.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object containing the authentication token")
public record LoginResponse(
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken) {}
