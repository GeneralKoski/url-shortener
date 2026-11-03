# Struttura del progetto

URL shortener: crei un link corto, chi lo apre viene reindirizzato e il click viene registrato.
Stack: Java 17, Spring Boot 4, MongoDB, Spring Security (JWT), Maven.

## Cartelle

```
pom.xml                 dipendenze e versione Java (come composer.json)
mvnw                    Maven Wrapper: scarica Maven da solo
docker-compose.yml      solo MongoDB, per lo sviluppo
local.properties        (ignorato da git) chiave JWT
src/main/java/...       codice dell'app
src/main/resources/     application.properties: configurazione
src/test/java/...       test
```

## Package (`dev.koski.urlshortener`)

Le cartelle coincidono con i package. Ogni cartella è un livello dell'app:

| Package | Ruolo | Contenuto |
|---|---|---|
| (radice) | Avvio | `UrlShortenerApplication`: il `main()` |
| `config` | Configurazione | Regole di sicurezza JWT, executor asincrono dei click |
| `model` | Dati salvati | `Link`, `Click`, `User` (record) |
| `repository` | Accesso a Mongo | Interfacce vuote, l'implementazione la genera Spring |
| `service` | Logica | `LinkService`, `StatsService`, `AuthService`, `ClickRecorder` |
| `web` | Entrata HTTP | Controller e record di richiesta/risposta |

## Flusso di una richiesta

```
HTTP -> web (controller) -> service -> repository -> MongoDB
        riceve e valida     logica     salva e legge
```

Esempio, `GET /ciao`:
1. `RedirectController` riceve la richiesta (pubblica, senza token).
2. `LinkService.visit` cerca il link, controlla la scadenza e passa il click a `ClickRecorder`.
3. `ClickRecorder` scrive `$inc` e il documento `Click` in background (`@Async`).
4. Il controller risponde 302 verso l'URL originale.

## Mappa con Laravel

| Qui | Laravel |
|---|---|
| Controller (`web`) | Controller |
| `*Request`, `*Response` (`web`) | FormRequest, API Resource |
| `service` | Service class |
| `repository` | Eloquent (qui sono interfacce) |
| `model` | Model |
| `config` | `config/` e service provider |

## Concetti Java da ricordare

- **Package = cartelle**; una classe pubblica per file, con lo stesso nome del file.
- **Annotazioni** (`@Service`, `@GetMapping`, `@Document`): dicono a Spring cosa fare con la classe o il metodo.
- **Dependency injection**: non si scrive `new LinkService()`. Spring crea gli oggetti (bean) e li passa nel costruttore.
- **Record**: classe di dati immutabile, con costruttore, getter, `equals` e `toString` generati.
- **Interfaccia repository**: si dichiara `findBySlug(...)` e Spring genera la query dal nome del metodo.
- **Request e Response separati dal model**: il documento Mongo (con `passwordHash`) non esce mai direttamente dall'API.

## Comandi

```
docker compose up -d      avvia Mongo
./mvnw spring-boot:run    avvia l'app (porta 8081)
./mvnw test               test con Mongo in Testcontainers
```
