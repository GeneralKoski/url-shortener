package dev.koski.urlshortener.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Instant;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.mongodb.client.result.UpdateResult;

import dev.koski.urlshortener.model.Click;
import dev.koski.urlshortener.model.Link;
import dev.koski.urlshortener.repository.ClickRepository;

@Component
public class ClickRecorder {

    private final MongoTemplate mongo;
    private final ClickRepository clicks;

    public ClickRecorder(MongoTemplate mongo, ClickRepository clicks) {
        this.mongo = mongo;
        this.clicks = clicks;
    }

    @Async("clickExecutor")
    public void record(String linkId, String slug, Instant at, String country, String device, String referrer) {
        UpdateResult result = mongo.updateFirst(query(where("id").is(linkId)), new Update().inc("clickCount", 1),
                Link.class);
        if (result.getMatchedCount() > 0) {
            clicks.insert(new Click(null, slug, at, country, device, referrer));
        }
    }
}
