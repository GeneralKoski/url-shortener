package dev.koski.urlshortener.web;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
