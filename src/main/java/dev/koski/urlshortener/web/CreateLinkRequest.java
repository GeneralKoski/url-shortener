package dev.koski.urlshortener.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank @Size(max = 2048) @Pattern(regexp = "https?://.+", message = "deve essere un URL http o https") String url,
        @Pattern(regexp = "[A-Za-z0-9_-]{3,32}", message = "da 3 a 32 caratteri tra lettere, numeri, - e _") String slug,
        List<@NotBlank String> tags,
        @Future Instant expiresAt) {
}
