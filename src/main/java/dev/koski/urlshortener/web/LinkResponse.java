package dev.koski.urlshortener.web;

import java.time.Instant;
import java.util.List;

import dev.koski.urlshortener.model.Link;

public record LinkResponse(String slug, String url, List<String> tags, Instant createdAt, Instant expiresAt,
        long clickCount) {

    public static LinkResponse from(Link link) {
        return new LinkResponse(link.slug(), link.url(), link.tags(), link.createdAt(), link.expiresAt(),
                link.clickCount());
    }
}
