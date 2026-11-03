package dev.koski.urlshortener.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("links")
@CompoundIndex(def = "{'owner': 1, 'createdAt': -1}")
public record Link(
        @Id String id,
        @Indexed(unique = true) String slug,
        String url,
        String owner,
        List<String> tags,
        Instant createdAt,
        @Indexed(expireAfter = "0s") Instant expiresAt,
        long clickCount) {
}
