package dev.koski.urlshortener.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import dev.koski.urlshortener.model.Link;
import dev.koski.urlshortener.service.LinkService;

@RestController
public class RedirectController {

    private final LinkService links;

    public RedirectController(LinkService links) {
        this.links = links;
    }

    @GetMapping("/{slug:[A-Za-z0-9_-]{3,32}}")
    public ResponseEntity<Void> redirect(
            @PathVariable String slug,
            @RequestHeader(value = "Referer", required = false) String referrer,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "CF-IPCountry", required = false) String country) {
        Link link = links.visit(slug, referrer, userAgent, country);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link.url())).build();
    }
}
