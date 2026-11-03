package dev.koski.urlshortener.web;

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
import org.springframework.web.bind.annotation.RequestHeader;
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
    public LinkResponse create(@RequestHeader("X-Owner") String owner, @Valid @RequestBody CreateLinkRequest request) {
        return LinkResponse.from(
                links.create(request.url(), request.slug(), request.tags(), request.expiresAt(), owner));
    }

    @GetMapping("/links")
    public PagedModel<LinkResponse> list(
            @RequestHeader("X-Owner") String owner,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(
                links.list(owner, tag, Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
                        .map(LinkResponse::from));
    }

    @GetMapping("/links/{slug}/stats")
    public LinkStats linkStats(
            @RequestHeader("X-Owner") String owner,
            @PathVariable String slug,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        Link link = links.owned(slug, owner);
        LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = from != null ? from : end.minusDays(29);
        return stats.forLink(link, start, end);
    }

    @GetMapping("/stats/tags")
    public List<TagStats> tagStats(@RequestHeader("X-Owner") String owner) {
        return stats.byTag(owner);
    }

    @DeleteMapping("/links/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Owner") String owner, @PathVariable String slug) {
        links.delete(slug, owner);
    }
}
