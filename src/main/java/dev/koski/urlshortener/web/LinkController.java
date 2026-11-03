package dev.koski.urlshortener.web;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.koski.urlshortener.model.Link;
import dev.koski.urlshortener.service.LinkService;
import dev.koski.urlshortener.service.StatsService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LinkController {

    private static final int MAX_PAGE_SIZE = 100;

    private final LinkService links;
    private final StatsService stats;

    public LinkController(LinkService links, StatsService stats) {
        this.links = links;
        this.stats = stats;
    }

    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(Principal principal, @Valid @RequestBody CreateLinkRequest request) {
        return LinkResponse.from(
                links.create(request.url(), request.slug(), request.tags(), request.expiresAt(), principal.getName()));
    }

    @GetMapping("/links")
    public PagedModel<LinkResponse> list(
            Principal principal,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(
                links.list(principal.getName(), tag, Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
                        .map(LinkResponse::from));
    }

    @GetMapping("/links/{slug}/stats")
    public LinkStats linkStats(
            Principal principal,
            @PathVariable String slug,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        Link link = links.owned(slug, principal.getName());
        LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = from != null ? from : end.minusDays(29);
        return stats.forLink(link, start, end);
    }

    @GetMapping("/stats/tags")
    public List<TagStats> tagStats(Principal principal) {
        return stats.byTag(principal.getName());
    }

    @DeleteMapping("/links/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable String slug) {
        links.delete(slug, principal.getName());
    }
}
