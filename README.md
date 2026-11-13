# GitHub Repository Searcher

Spring Boot 3 / Java 17 REST service that searches GitHub repositories through the
[GitHub REST API](https://docs.github.com/en/rest/search/search#search-repositories), stores (upserts) the
results in PostgreSQL, and exposes a filterable endpoint over the stored data. No UI – test with Swagger, Postman or curl.

## Live demo

| | |
|---|---|
| **Live URL** | https://githubsearcher-zjvg.onrender.com |
| **Swagger UI (API docs)** | https://githubsearcher-zjvg.onrender.com/swagger-ui.html |
| **OpenAPI JSON** | https://githubsearcher-zjvg.onrender.com/v3/api-docs |

> **Note:** the app is hosted on Render's free tier and goes to sleep after ~15 minutes without traffic.
> The first request after that can take up to a minute while it wakes up – please wait and refresh.

Quick try (live):
```bash
curl -X POST https://githubsearcher-zjvg.onrender.com/api/github/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"spring boot","language":"Java","sort":"stars"}'

curl 'https://githubsearcher-zjvg.onrender.com/api/github/repositories?language=Java&minStars=100&sort=stars'
```

## Tech stack
Java 17 · Spring Boot 3.2 · Spring Data JPA · PostgreSQL · Springdoc OpenAPI (Swagger) · JUnit 5 · Mockito · JaCoCo · Docker

## Run locally

### Prerequisites
* JDK 17+
* Maven 3.9+
* PostgreSQL 13+ (installed locally **or** via the bundled Docker Compose file)

### 1. Database
Either install PostgreSQL locally and create an empty database:
```sql
CREATE DATABASE github_searcher;
```
(defaults expected: `localhost:5432`, user `postgres`, password `postgres`)

or start it with Docker:
```bash
docker compose up -d
```
Tables are created automatically on first start (`docs/schema.sql` shows the equivalent DDL).

### 2. GitHub token (optional)
Without a token GitHub allows ~10 search requests/minute; with one, ~30.
Set it as an environment variable (never commit it):
```powershell
$env:GITHUB_TOKEN="ghp_your_token_here"      # Windows PowerShell
export GITHUB_TOKEN=ghp_your_token_here      # macOS / Linux
```
In IntelliJ: Run/Debug Configurations → Modify options → Environment variables.

### 3. Start the app
```bash
mvn spring-boot:run
```
The API runs at **http://localhost:8082**

* Swagger UI: http://localhost:8082/swagger-ui.html
* Postman: import `docs/GitHub-Repository-Searcher.postman_collection.json` (set the `baseUrl` variable to the local or live URL)

### Configuration (environment variables)
| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/github_searcher` | JDBC URL (must start with `jdbc:`) |
| `DB_USERNAME` | `postgres` | database user |
| `DB_PASSWORD` | `postgres` | database password |
| `GITHUB_TOKEN` | *(empty)* | optional GitHub token |
| `PORT` | `8082` | HTTP port (set automatically by Render) |

## Deployment
* **App:** [Render](https://render.com) web service built from the `Dockerfile` (multi-stage Maven build → slim JRE image).
* **Database:** [Neon](https://neon.tech) serverless PostgreSQL.
* Credentials are supplied only as environment variables in Render (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GITHUB_TOKEN`) – none are stored in the repository.

## Tests
```bash
mvn test                              # unit + slice + integration tests (H2 in-memory, GitHub is mocked – no network needed)
open target/site/jacoco/index.html    # coverage report
```

## API

### 1. `POST /api/github/search` – fetch from GitHub and save
```bash
curl -X POST http://localhost:8082/api/github/search -H 'Content-Type: application/json' \
  -d '{"query":"spring boot","language":"Java","sort":"stars"}'
```
| Field | Required | Notes |
|---|---|---|
| `query` | yes | 1–256 chars. Matched against the repository **name** (partial or full) via GitHub's `in:name`; supply your own `in:` qualifier to override |
| `language` | no | e.g. `Java`, `C++`, `Jupyter Notebook` |
| `sort` | no | `stars`, `forks` or `updated`; omitted = GitHub best match |
| `perPage` | no | 1–100, default 30 |
| `page` | no | ≥ 1, default 1 |

Response `200`:
```json
{
  "message": "Repositories fetched and saved successfully",
  "repositories": [{
    "id": 123456, "name": "spring-boot-example", "description": "An example repository for Spring Boot",
    "owner": "user123", "language": "Java", "stars": 450, "forks": 120,
    "lastUpdated": "2024-01-01T12:00:00Z",
    "url": "https://github.com/user123/spring-boot-example", "watchers": 450, "openIssues": 7,
    "sizeKb": 2048, "license": "MIT License", "createdAt": "2020-01-01T00:00:00Z", "pushedAt": "2024-01-01T11:00:00Z"
  }]
}
```
No matches → `200` with `"message": "No repositories found for the given criteria"` and `"repositories": []`.

### 2. `GET /api/github/repositories` – read stored data
```bash
curl 'http://localhost:8082/api/github/repositories?language=Java&minStars=100&sort=stars'
```
| Param | Notes |
|---|---|
| `language` | optional, case-insensitive exact match |
| `minStars` | optional, integer ≥ 0 |
| `sort` | optional: `stars` (default), `forks`, `updated` – always descending |
| `limit` | optional, 1–1000, default 100 (safety cap, not in the original spec) |

Response: `{ "repositories": [ ... same objects as above ... ] }`

### Errors
Every error has the same shape:
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Request validation failed",
  "path": "/api/github/search", "details": ["query: must not be blank"] }
```
| Situation | HTTP |
|---|---|
| Blank/oversized `query`, bad `language`, bad `sort`, bad `minStars`, malformed JSON, GitHub 422 | 400 |
| Wrong content type / method | 415 / 405 |
| GitHub rate limit (403 with `X-RateLimit-Remaining: 0`, or 429) – includes `Retry-After` when GitHub supplies it | 429 |
| GitHub down, timeout, 401/403/5xx, unreadable or unexpected payload | 502 |
| Database / unexpected failure (details only in logs) | 500 |

## Design
```
controller  -> thin REST layer, validation (@Valid), no logic
service     -> GithubSearchService (orchestration), RepositoryUpsertService (@Transactional write),
               RepositoryQueryService (read, filters)
client      -> GithubApiClient (port) + GithubApiClientImpl (RestClient adapter, maps HTTP failures to domain exceptions)
repository  -> Spring Data JPA + Specification pattern (RepositorySpecifications)
mapper      -> GitHub JSON <-> entity <-> response DTO
util        -> SortField (strategy enum: API value / GitHub value / entity property), GithubQueryBuilder
exception   -> domain exceptions + GlobalExceptionHandler (@RestControllerAdvice)
```
* **Efficient upsert** – the primary key is GitHub's repository id. One `SELECT … WHERE id IN (…)` loads existing rows,
  they are updated in place (Hibernate dirty checking) and new ones inserted; JDBC batching is on. `GithubRepository`
  implements `Persistable` so new rows are plain `INSERT`s with no extra SELECT per row.
* **No DB connection held during the HTTP call** – `GithubSearchService` is intentionally not transactional.
* Extra statistics stored beyond the required fields: URL, watchers, open issues, size, license, created/pushed
  dates and the time of the last fetch.
* Indexes on `language`, `stars_count`, `forks_count`, `last_updated` support the retrieval filters/sorts.

## Assumptions / limits
* GitHub's search API returns at most 1000 results per query; use `page`/`perPage` to walk through them.
* Unauthenticated GitHub search is limited to 10 requests/min – set `GITHUB_TOKEN` for real use.
* Two concurrent searches inserting the *same new* repository could collide on the primary key; the loser gets a
  500 and a retry succeeds. (Postgres `INSERT … ON CONFLICT` would remove that window; JPA-portable code was
  preferred here so the same tests run on H2.)