# CodeArena

CodeArena is a backend REST API for a competitive coding-contest platform: users register,
browse problems, join time-boxed contests, submit solutions, and see a live leaderboard.
It's a portfolio/resume project — the goal is a clean, correctly-layered Spring Boot API
with real auth, real business rules (contest timing, duplicate-join prevention, scoring),
and a Postman/Swagger-documented surface. v1 does not execute submitted code; submissions
are persisted as data with a status/score.

## Tech stack

- Java 21, Spring Boot 3.x, Maven (Maven Wrapper — use `mvnw`/`mvnw.cmd`, not a system `mvn`)
- Spring Web, Spring Data JPA, Spring Security (stateless JWT), Validation, Lombok
- MySQL (local instance, db name `codearena_db`)
- springdoc-openapi-starter-webmvc-ui (Swagger UI) for API docs
- Postman collection for regression testing

## Package structure (under `com.codearena`)

```
controller/   REST endpoints — thin, delegate to services
service/      business logic (scoring, leaderboard, contest state, etc.)
repository/   Spring Data JPA repositories
entity/       JPA entities — never returned directly from a controller
dto/          request/response DTOs with validation annotations
security/     SecurityConfig, JwtService, JwtAuthenticationFilter
exception/    custom exceptions + @RestControllerAdvice global handler
```

## Source of truth

- **[CodeArena_Build_Guide.md](CodeArena_Build_Guide.md)** — the 14-phase roadmap. Each phase has
  a ready-to-paste prompt and a verify checklist. Don't skip ahead; later phases assume earlier
  layers exist.
- **[PROGRESS.md](PROGRESS.md)** — what's actually been built, phase by phase, plus any
  decisions/deviations from the guide and what's next.

**At the start of every session, read both files first** before doing new work, to see what's
already done and what the next phase is.

## Workflow

After finishing each phase: check it off in PROGRESS.md, note what was built and any deviations,
then make a git commit for that phase (roughly one meaningful commit per phase, per the guide's
Phase 14 git-hygiene goal).
