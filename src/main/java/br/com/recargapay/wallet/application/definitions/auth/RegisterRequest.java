package br.com.recargapay.wallet.application.definitions.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for customer registration")
public record RegisterRequest(
    @Schema(
            description = "Customer's full name",
            example = "John Doe",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String fullName,
    @Schema(
            description = "Customer's email address",
            example = "john.doe@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        String email,
    @Schema(
            description = "Customer's password",
            example = "securePassword123",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String password) {}
