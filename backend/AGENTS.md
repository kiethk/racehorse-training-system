# Backend conventions

Spring Boot service under `com.rtms.backend`. Domain vocabulary lives in `../GLOSSARY.md`; decisions in `../docs/adr/`. Use those terms (for example Course Run, Exercise), not the words under `_Avoid_`.

## Structure

- One package per module (`training`, `stable`, `horse`, `admission`, ...). Inside each, layered subpackages: `controller`, `dto`, `entity`, `repository`, `service`.
- A service called by a controller or by another module has an interface plus a `...Impl` class. Internal helpers are package-private classes with no interface.
- Controllers stay thin: bind the request, check authority, call one service, wrap the result.
- Entities have **no JPA relations** (`@ManyToOne`, `@OneToMany`): reference other rows by plain id columns and load them with repositories.
- DTOs are Java `record`s. Do not return entities from controllers.

## API

- Responses are wrapped in `common.dto.ApiResponse`.
- Errors are thrown as `config.ApiException(HttpStatus, "ERROR_CODE", message)`; the code is `UPPER_SNAKE_CASE`, prefixed by the resource (`COURSE_RUN_NOT_FOUND`).
- Authorise with `@PreAuthorize("hasAuthority('RESOURCE_VERB')")`, for example `ADMISSION_DOCUMENT_VIEW`. Every authority used must exist in the `permissions` table, granted through a Flyway migration.
- Size limits and other training constants are declared once in `config/TrainingLimits`; requests validate with Bean Validation against them and `/api/training-rules` exposes them. Do not repeat a magic number in another class.

## Comments

Write Vietnamese first, then the English translation in parentheses:

```java
// Hủy các buổi chưa diễn ra (cancel sessions that have not happened yet)
```

## Database

- Schema changes are Flyway migrations in `src/main/resources/db/migration/`. **Never edit a migration that is already committed**; add a new one with a later version number.
- Renaming a table does not rewrite function bodies, constraint names or index names: update them in the same migration.
- `ddl-auto` is `validate`, so a wrong table or column name fails at startup.

## Tests

- Tests run against a real Postgres, not H2. Run them on a **clone** of the dev database, never on `rtms_db` itself: create a new database from `pg_dump rtms_db`, then run Maven with `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_PASSWORD` pointing to the clone.
- Time-dependent logic uses the injected `Clock` (`TestClockConfig` / `MutableClock` in tests), never `LocalDate.now()` directly.
- Raw SQL (native queries, schema tests) is only checked at runtime, so cover it with a test.
