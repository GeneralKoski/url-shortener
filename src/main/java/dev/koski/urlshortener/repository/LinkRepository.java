package dev.koski.urlshortener.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import dev.koski.urlshortener.model.Link;

public interface LinkRepository extends MongoRepository<Link, String> {

    Optional<Link> findBySlug(String slug);

    Optional<Link> findBySlugAndOwner(String slug, String owner);

    Page<Link> findByOwner(String owner, Pageable pageable);

    @Query("{ 'owner': ?0, 'tags': ?1 }")
    Page<Link> findByOwnerAndTag(String owner, String tag, Pageable pageable);

    long deleteBySlug(String slug);
}
