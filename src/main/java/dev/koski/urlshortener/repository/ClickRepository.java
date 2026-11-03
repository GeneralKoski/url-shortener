package dev.koski.urlshortener.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import dev.koski.urlshortener.model.Click;

public interface ClickRepository extends MongoRepository<Click, String> {

    long deleteBySlug(String slug);
}
