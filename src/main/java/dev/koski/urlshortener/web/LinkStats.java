package dev.koski.urlshortener.web;

import java.util.List;

public record LinkStats(
        String slug,
        long totalClicks,
        long clicksInRange,
        List<Bucket> perDay,
        List<Bucket> referrers,
        List<Bucket> devices,
        List<Bucket> countries) {
}
