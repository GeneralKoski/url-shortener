# url-shortener

Mini progetto per provare Spring Boot 4 + MongoDB: URL shortener con statistiche sui click.

## Avvio

Opzione A - Mongo in Docker:

    docker compose up -d
    ./mvnw spring-boot:run

Opzione B - senza compose (Mongo avviato da Testcontainers):

    ./mvnw spring-boot:test-run

Test:

    ./mvnw test

## API (header `X-Owner` obbligatorio su /api)

    curl -X POST localhost:8081/api/links -H 'X-Owner: martin' -H 'Content-Type: application/json' \
      -d '{"url":"https://example.com","slug":"ciao","tags":["demo"]}'
    curl -i localhost:8081/ciao
    curl localhost:8081/api/links -H 'X-Owner: martin'
    curl localhost:8081/api/links/ciao/stats -H 'X-Owner: martin'
    curl localhost:8081/api/stats/tags -H 'X-Owner: martin'
    curl -X DELETE localhost:8081/api/links/ciao -H 'X-Owner: martin'
