package dev.koski.urlshortener.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,32}", message = "da 3 a 32 caratteri tra lettere, numeri, . - e _") String username,
        @NotBlank @Size(min = 8, max = 72, message = "da 8 a 72 caratteri") String password) {
}
