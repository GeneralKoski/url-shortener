# url-shortener

Mini progetto per provare Spring Boot 4 + MongoDB: URL shortener con statistiche sui click e autenticazione JWT.

## Avvio

Opzione A - Mongo in Docker:

    docker compose up -d
    ./mvnw spring-boot:run

Opzione B - senza compose (Mongo avviato da Testcontainers):

    ./mvnw spring-boot:test-run

Test:

    ./mvnw test

## Configurazione

- `JWT_SECRET`: chiave per firmare i token, almeno 32 caratteri. Se manca l'app ne genera una casuale
  a ogni avvio (i token non sopravvivono al riavvio) e lo scrive nel log.
- `app.jwt.ttl`: durata del token, default `1h`.

## API

Registrazione e login (pubblici):

    curl -X POST localhost:8081/api/auth/register -H 'Content-Type: application/json' \
      -d '{"username":"martin","password":"una-password-lunga"}'
    curl -X POST localhost:8081/api/auth/login -H 'Content-Type: application/json' \
      -d '{"username":"martin","password":"una-password-lunga"}'

Il login restituisce `accessToken`. Mettilo in una variabile e usalo come Bearer:

    TOKEN=<accessToken>
    curl -X POST localhost:8081/api/links -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
      -d '{"url":"https://example.com","slug":"ciao","tags":["demo"]}'
    curl -i localhost:8081/ciao
    curl localhost:8081/api/links -H "Authorization: Bearer $TOKEN"
    curl localhost:8081/api/links/ciao/stats -H "Authorization: Bearer $TOKEN"
    curl localhost:8081/api/stats/tags -H "Authorization: Bearer $TOKEN"
    curl -X DELETE localhost:8081/api/links/ciao -H "Authorization: Bearer $TOKEN"

Il redirect `GET /{slug}` è pubblico, tutto sotto `/api` (tranne `/api/auth`) richiede il token.
