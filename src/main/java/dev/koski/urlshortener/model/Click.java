package dev.koski.urlshortener.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("clicks")
@CompoundIndex(def = "{'slug': 1, 'at': 1}")
public record Click(
        @Id String id,
        String slug,
        Instant at,
        String country,
        String device,
        String referrer) {
}
