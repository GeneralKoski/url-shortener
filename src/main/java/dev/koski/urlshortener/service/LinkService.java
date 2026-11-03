package dev.koski.urlshortener.service;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.koski.urlshortener.model.Link;
import dev.koski.urlshortener.repository.ClickRepository;
import dev.koski.urlshortener.repository.LinkRepository;

@Service
public class LinkService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int GENERATED_SLUG_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;

    private final LinkRepository links;
    private final ClickRepository clicks;
    private final ClickRecorder recorder;
    private final SecureRandom random = new SecureRandom();

    public LinkService(LinkRepository links, ClickRepository clicks, ClickRecorder recorder) {
        this.links = links;
        this.clicks = clicks;
        this.recorder = recorder;
    }

    public Link create(String url, String customSlug, List<String> tags, Instant expiresAt, String owner) {
        boolean custom = customSlug != null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String slug = custom ? customSlug : randomSlug();
            try {
                return links.insert(new Link(null, slug, url, owner, tags == null ? List.of() : tags,
                        Instant.now(), expiresAt, 0));
            } catch (DuplicateKeyException e) {
                if (custom) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug già in uso");
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile generare uno slug univoco");
    }

    public Link visit(String slug, String referrer, String userAgent, String country) {
        Link link = links.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link non trovato"));
        if (link.expiresAt() != null && link.expiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Link scaduto");
        }
        recorder.record(slug, Instant.now(), country == null ? "unknown" : country,
                device(userAgent), referrerHost(referrer));
        return link;
    }

    public Page<Link> list(String owner, String tag, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return tag == null ? links.findByOwner(owner, pageable) : links.findByOwnerAndTag(owner, tag, pageable);
    }

    public Link owned(String slug, String owner) {
        return links.findBySlugAndOwner(slug, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link non trovato"));
    }

    public void delete(String slug, String owner) {
        owned(slug, owner);
        links.deleteBySlug(slug);
        clicks.deleteBySlug(slug);
    }

    private String randomSlug() {
        StringBuilder sb = new StringBuilder(GENERATED_SLUG_LENGTH);
        for (int i = 0; i < GENERATED_SLUG_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String device(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "tablet";
        }
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        }
        return "desktop";
    }

    private String referrerHost(String referrer) {
        if (referrer == null) {
            return "direct";
        }
        try {
            String host = URI.create(referrer).getHost();
            return host == null ? "direct" : host;
        } catch (IllegalArgumentException e) {
            return "direct";
        }
    }
}
