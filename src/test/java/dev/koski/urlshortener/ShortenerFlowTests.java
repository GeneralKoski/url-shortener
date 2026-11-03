package dev.koski.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import dev.koski.urlshortener.model.Link;
import dev.koski.urlshortener.repository.ClickRepository;
import dev.koski.urlshortener.repository.LinkRepository;
import dev.koski.urlshortener.service.LinkService;
import dev.koski.urlshortener.service.StatsService;
import dev.koski.urlshortener.web.Bucket;
import dev.koski.urlshortener.web.LinkStats;
import dev.koski.urlshortener.web.TagStats;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShortenerFlowTests {

    @Autowired LinkService links;
    @Autowired StatsService stats;
    @Autowired LinkRepository linkRepository;
    @Autowired ClickRepository clickRepository;
    @Autowired WebApplicationContext context;

    @BeforeEach
    void clean() {
        linkRepository.deleteAll();
        clickRepository.deleteAll();
    }

    private void awaitClicks(long expected) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(clickRepository.count()).isEqualTo(expected);
            assertThat(linkRepository.findAll().stream().mapToLong(Link::clickCount).sum()).isEqualTo(expected);
        });
    }

    @Test
    void duplicateCustomSlugIsConflict() {
        links.create("https://example.com", "mio-link", null, null, "martin");

        assertThatThrownBy(() -> links.create("https://example.org", "mio-link", null, null, "martin"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void generatedSlugIsCreated() {
        Link link = links.create("https://example.com", null, List.of("a"), null, "martin");

        assertThat(link.slug()).hasSize(7);
    }

    @Test
    void expiredLinkIsGone() {
        links.create("https://example.com", "scaduto", null, Instant.now().minus(1, ChronoUnit.HOURS), "martin");

        assertThatThrownBy(() -> links.visit("scaduto", null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.GONE));
    }

    @Test
    void redirectReturns302AndCountsClick() throws Exception {
        links.create("https://example.com/pagina", "ciao", null, null, "martin");
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(get("/ciao")).andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/pagina"));

        awaitClicks(1);
        assertThat(links.owned("ciao", "martin").clickCount()).isEqualTo(1);
    }

    @Test
    void statsAggregateClicks() {
        links.create("https://example.com", "stat", null, null, "martin");
        links.visit("stat", "https://news.ycombinator.com/item?id=1", "Mozilla/5.0 (iPhone) Mobile", "IT");
        links.visit("stat", "https://news.ycombinator.com/", "Mozilla/5.0 (iPhone) Mobile", "IT");
        links.visit("stat", null, "Mozilla/5.0 (Macintosh)", "US");
        awaitClicks(3);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LinkStats result = stats.forLink(links.owned("stat", "martin"), today.minusDays(1), today);

        assertThat(result.totalClicks()).isEqualTo(3);
        assertThat(result.clicksInRange()).isEqualTo(3);
        assertThat(result.perDay()).containsExactly(new Bucket(today.toString(), 3));
        assertThat(result.referrers().get(0)).isEqualTo(new Bucket("news.ycombinator.com", 2));
        assertThat(result.devices().get(0)).isEqualTo(new Bucket("mobile", 2));
        assertThat(result.countries().get(0)).isEqualTo(new Bucket("IT", 2));
    }

    @Test
    void tagStatsSumClicksPerTag() {
        links.create("https://a.com", "aaa", List.of("work", "dev"), null, "martin");
        links.create("https://b.com", "bbb", List.of("work"), null, "martin");
        links.create("https://c.com", "ccc", List.of("work"), null, "altro");
        links.visit("aaa", null, null, null);
        links.visit("aaa", null, null, null);
        links.visit("bbb", null, null, null);
        awaitClicks(3);

        List<TagStats> result = stats.byTag("martin");

        assertThat(result).containsExactly(new TagStats("work", 2, 3), new TagStats("dev", 1, 2));
    }
}
