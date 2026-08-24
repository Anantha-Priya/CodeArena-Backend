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
  **To be rectified in Phase 5** — a real root password shouldn't sit in git history even for
  a local-only project.
- Local Maven 3.9.16 install found at `C:\Users\IceMu\.m2\wrapper\dists\...` was reused to
  generate the wrapper — no need to install Maven system-wide.

**Next:** Phase 2 — JPA entity layer (User, Problem, Contest, ContestProblem,
ContestParticipant, Submission).

---

## Phase 2: Entity Layer & Database Design
- [x] Done

**Built:**
- Six JPA entities under `com.codearena.entity`: [User.java](src/main/java/com/codearena/entity/User.java),
  [Problem.java](src/main/java/com/codearena/entity/Problem.java), [Contest.java](src/main/java/com/codearena/entity/Contest.java),
  [ContestProblem.java](src/main/java/com/codearena/entity/ContestProblem.java),
  [ContestParticipant.java](src/main/java/com/codearena/entity/ContestParticipant.java),
  [Submission.java](src/main/java/com/codearena/entity/Submission.java) — matching `@Table` names
  `users`, `problems`, `contests`, `contest_problems`, `contest_participants`, `submissions`.
- Three enums (`Role`, `Difficulty`, `SubmissionStatus`), stored as `EnumType.STRING`.
- Lombok `@Getter/@Setter/@Builder` on every entity; `@ManyToOne(FetchType.LAZY)` +
  `@JoinColumn` for all FK relationships.
- Unique constraints: `users(username)`, `users(email)`, `contest_problems(contest_id, problem_id)`,
  `contest_participants(user_id, contest_id)`.
- `created_at` / `joined_at` / `submitted_at` auto-populated via Hibernate `@CreationTimestamp`.
- Verified against MySQL directly: `SHOW TABLES`, `information_schema.KEY_COLUMN_USAGE` (all 7 FKs
  present), `information_schema.STATISTICS` (all 4 unique constraints present) — all match the guide.

**Decisions/deviations:**
- None — built to spec as written.

**Next:** Phase 3 — repository interfaces (UserRepository, ProblemRepository, ContestRepository,
ContestParticipantRepository, SubmissionRepository).

---

## Phase 3: Repository Layer
- [x] Done

**Built:**
- [UserRepository.java](src/main/java/com/codearena/repository/UserRepository.java) —
  `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`.
- [ProblemRepository.java](src/main/java/com/codearena/repository/ProblemRepository.java) —
  `JpaRepository` (gives paginated `findAll(Pageable)` for free) + `JpaSpecificationExecutor`.
- [ContestRepository.java](src/main/java/com/codearena/repository/ContestRepository.java) —
  standard CRUD, no extra methods.
- [ContestParticipantRepository.java](src/main/java/com/codearena/repository/ContestParticipantRepository.java) —
  `existsByUserIdAndContestId`, `findByContestId`.
- [SubmissionRepository.java](src/main/java/com/codearena/repository/SubmissionRepository.java) —
  `findByUserId`, `findByContestIdAndStatus`, `findByUserIdOrderBySubmittedAtDesc`.
- Verified: app boots with "Found 5 JPA repository interfaces" and no query-derivation errors.

**Decisions/deviations:**
- `ProblemRepository` uses `JpaSpecificationExecutor` instead of separate `findByDifficulty`/
  `findByTopic` derived methods — the guide explicitly offers this as an alternative. Phase 8
  needs difficulty, topic, and pagination combined as independently-optional filters
  (`?difficulty=&topic=&page=&size=`), which a `Specification` handles directly instead of
  needing a derived method for every filter combination.

**Next:** Phase 4 — request/response DTOs with validation.

---

## Phase 4: DTOs & Validation
- [ ] Done

**Built:**

**Decisions/deviations:**

**Next:**

---

## Phase 5: Security Layer (Spring Security + JWT)
- [ ] Done

**Carried over from Phase 1 (fix as part of this phase, not just the guide's own scope):**
- Replace the temporary permit-all `SecurityConfig` with the real rule set: public
  `/api/auth/**`, `/api/health`, Swagger paths; everything else authenticated.
- Remove the hardcoded `root@123` DB password fallback from `application.properties` —
  move real local credentials to a gitignored `application-local.properties`
  (`spring.profiles.active=local`), commit an `application-local.properties.example`
  template instead.

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
