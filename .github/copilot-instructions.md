# Cashmere Transfer Service — Copilot Instructions

Spring Boot (Java 21, Spring Boot 4.1) service that transfers Omnipub content (articles,
videos, podcasts) from source systems (S3, GitHub, an internal Content API) into the
Cashmere API, driven by CSV uploads.

## Build, test, run

- Build: `./gradlew build`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "com.hbr.cashmere.transfer_service.service.CsvServiceTest"`
- Run a single test method: `./gradlew test --tests "com.hbr.cashmere.transfer_service.service.CsvServiceTest.deleteOmnipubs_ShouldNotDeleteWhen_ProductStateIsApproved"`
- Test reports: `build/reports/tests/test/index.html`
- Run app (sandbox, default profile): `./gradlew bootRun`
- Run app (prod profile): `./gradlew bootRun --args='--spring.profiles.active=prod'`
- Clean rebuild if a build looks stale: `./gradlew clean build`

## Architecture

Request flow: `CsvUploadController` → `CsvService` (orchestrator) → `S3FileService`,
`ContentService`, `GitService`, `CashmereService`.

- **CsvUploadController** (`controller/`): REST endpoints under `/api/csv` for uploading
  content CSVs (`/upload`), deletion manifests (`/delete`), and local XML files
  (`/process-local-xml`). Dispatches to video vs. non-video row types based on whether the
  uploaded filename contains `"CL-Videos"`.
- **CsvService** (`service/`): the core orchestrator. For each CSV row it downloads the
  source XML (from S3 or GitHub depending on content type), extracts/cleans metadata via
  `XmlUtil`, optionally enriches with `ContentService` metadata, then creates/updates/deletes
  Omnipubs via `CashmereService` and manages collection membership.
- **S3FileService**: downloads XML from S3 and delegates to `XmlUtil.processXml` to strip
  images and clean HTML before further processing.
- **GitService**: fetches video XML files (and their last-commit metadata) from the
  `Corporate-Learning/cl-content-media` GitHub repo, `prod-release` branch.
- **ContentService**: calls an internal Content API to fetch supplemental metadata when the
  source XML lacks author info.
- **CashmereService**: the Cashmere API client — creates/updates/deletes Omnipubs and
  adds/removes them from collections. Builds multipart bodies manually (`buildMultipartBody`)
  rather than relying on Spring's multipart support, since `WebClient` needs raw control over
  the boundary/content-type per part.
- Each external API dependency gets its own `WebClient` bean in `WebClientConfig`, wired via
  `@Qualifier` (`cashmereWebClient`, `contentWebClient`, `githubWebClient`) — when adding a new
  external integration, add a new qualified `WebClient` bean rather than reusing an existing one.

### Environment-based collection IDs

Collection IDs and API tokens differ between sandbox (default) and prod, driven by Spring
profiles (`application-sandbox.properties` / `application-prod.properties`) and bound via
`CollectionIdProperties` (`@ConfigurationProperties(prefix = "collections")`).
`CollectionConstants` exposes these as static getters for legacy call sites — it wraps a
static field populated at construction, so it depends on the `CollectionIdProperties` bean
existing in the Spring context (won't work standalone in a plain unit test without wiring it).
See `ENVIRONMENT_CONFIG.md` for the full sandbox/prod collection ID mapping and how to switch
profiles (program args, VM options, env var, or IntelliJ run config).

## Conventions

- JSON handling uses `tools.jackson.databind` (Jackson 3.x), not the legacy
  `com.fasterxml.jackson` package — keep imports consistent with this when adding new code.
- Reactive types (`Mono`/`Flux` via WebFlux) are used throughout the service layer for external
  API calls; `CashmereService`/`GitService`/`ContentService` return `Mono<...>` and callers in
  `CsvService` `.block()` or `.subscribe()` as appropriate — avoid introducing blocking HTTP
  clients alongside these.
- Cashmere API error handling follows a consistent pattern: `onStatus` handlers for
  4xx/5xx map to `RuntimeException` using message templates from `ErrorConstants`
  (`CLIENT_ERROR_BODY`/`SERVER_ERROR_BODY` for logging, `CLIENT_ERROR`/`SERVER_ERROR` for the
  exception message) — reuse this pattern for new Cashmere endpoints.
- Constants live in `constants/` by concern (`CsvConstants`, `DeleteConstants`, `ErrorConstants`,
  `XmlConstants`, `CollectionConstants`) rather than inline literals — add new shared strings
  there.
- Tests use JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) and MockWebServer for
  WebClient-based service tests. `CsvServiceTest` intentionally only covers
  validation/business logic that doesn't involve async `subscribe()` calls or static utility
  methods (`XmlUtil`, `CsvUtil`) — see the class-level comment there before adding more
  end-to-end style tests to it.
- Never commit real API tokens; `application-prod.properties` should only ever contain
  placeholder tokens locally.
