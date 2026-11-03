package dev.koski.urlshortener.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import dev.koski.urlshortener.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);
}
