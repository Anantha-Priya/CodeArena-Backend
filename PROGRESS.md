# CodeArena — Progress

Tracks actual build status against [CodeArena_Build_Guide.md](CodeArena_Build_Guide.md).
Check a phase off when its verify checklist passes and the commit is made.

## Phase 1: Project Setup & Skeleton
- [x] Done

**Built:**
- Spring Boot 3.3.4 / Java 21 Maven project, group `com.codearena`, artifact `codearena`.
- Dependencies: Spring Web, Spring Data JPA, Spring Security, Validation, Lombok,
  mysql-connector-j, springdoc-openapi-starter-webmvc-ui.
- Empty package skeleton under `com.codearena`: controller, service, repository, entity,
  dto, security, exception.
- `application.properties`: MySQL connection to `codearena_db` (localhost:3306,
  `createDatabaseIfNotExist=true`), `ddl-auto=update`, `server.port=8080`, Swagger UI path.
- `GET /api/health` → `{"status":"UP"}` ([HealthController.java](src/main/java/com/codearena/controller/HealthController.java)).
- Maven Wrapper (`mvnw`/`mvnw.cmd`) pinned to Maven 3.9.16 so the project builds without a
  system-installed Maven.

**Decisions/deviations:**
- Added a temporary permit-all `SecurityConfig` ([SecurityConfig.java](src/main/java/com/codearena/security/SecurityConfig.java)).
  Not in the guide's Phase 1 prompt, but required — pulling in `spring-boot-starter-security`
  with zero config locks every endpoint (including `/api/health`) behind a random generated
  password, which fails the phase's own verify step. This file gets fully replaced in Phase 5.
- DB credentials: `spring.datasource.username`/`password` read from `DB_USERNAME`/`DB_PASSWORD`
  env vars, falling back to `root`/`root@123` (the local dev MySQL80 instance) for convenience.
  **Before this repo goes public (Phase 14), replace the password fallback** — a real root
  password shouldn't sit in git history even for a local-only project.
- Local Maven 3.9.16 install found at `C:\Users\IceMu\.m2\wrapper\dists\...` was reused to
  generate the wrapper — no need to install Maven system-wide.

**Next:** Phase 2 — JPA entity layer (User, Problem, Contest, ContestProblem,
ContestParticipant, Submission).

---

## Phase 2: Entity Layer & Database Design
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 3: Repository Layer
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 4: DTOs & Validation
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 5: Security Layer (Spring Security + JWT)
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 6: Authentication Module
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 7: Global Exception Handling
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 8: Problem Management Module
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 9: Contest Management Module
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 10: Contest Participation
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 11: Submission Module + Score Calculation
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 12: Leaderboard, Profile & Rating
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 13: API Documentation + Testing
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 14: Git Hygiene & Resume Polish
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**
