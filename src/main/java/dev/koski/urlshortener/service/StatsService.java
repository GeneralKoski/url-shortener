package dev.koski.urlshortener.service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.facet;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import dev.koski.urlshortener.model.Link;
import dev.koski.urlshortener.web.Bucket;
import dev.koski.urlshortener.web.LinkStats;
import dev.koski.urlshortener.web.TagStats;

@Service
public class StatsService {

    private static final int TOP = 5;

    private final MongoTemplate mongo;

    public StatsService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public LinkStats forLink(Link link, LocalDate from, LocalDate to) {
        Criteria range = where("slug").is(link.slug())
                .and("at").gte(from.atStartOfDay(ZoneOffset.UTC).toInstant())
                .lt(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        List<Bucket> perDay = buckets(mongo.aggregate(newAggregation(
                match(range),
                project().and(DateOperators.dateOf("at").toString("%Y-%m-%d")).as("day"),
                group("day").count().as("count")), "clicks", Document.class).getMappedResults())
                .stream().sorted(Comparator.comparing(Bucket::key)).toList();

        Document facets = mongo.aggregate(newAggregation(
                match(range),
                facet(group("referrer").count().as("count")).as("referrers")
                        .and(group("device").count().as("count")).as("devices")
                        .and(group("country").count().as("count")).as("countries")),
                "clicks", Document.class).getUniqueMappedResult();

        return new LinkStats(
                link.slug(),
                link.clickCount(),
                perDay.stream().mapToLong(Bucket::count).sum(),
                perDay,
                top(facets, "referrers"),
                top(facets, "devices"),
                top(facets, "countries"));
    }

    public List<TagStats> byTag(String owner) {
        return mongo.aggregate(newAggregation(
                match(where("owner").is(owner)),
                unwind("tags"),
                group("tags").count().as("links").sum("clickCount").as("clicks"),
                sort(Sort.Direction.DESC, "clicks")), "links", Document.class).getMappedResults()
                .stream()
                .map(d -> new TagStats(d.getString("_id"), number(d, "links"), number(d, "clicks")))
                .toList();
    }

    private List<Bucket> top(Document facets, String name) {
        if (facets == null) {
            return List.of();
        }
        return buckets(facets.getList(name, Document.class)).stream()
                .sorted(Comparator.comparingLong(Bucket::count).reversed())
                .limit(TOP)
                .toList();
    }

    private List<Bucket> buckets(List<Document> docs) {
        return docs.stream().map(d -> new Bucket(String.valueOf(d.get("_id")), number(d, "count"))).toList();
    }

    private long number(Document d, String field) {
        return ((Number) d.get(field)).longValue();
    }
}
