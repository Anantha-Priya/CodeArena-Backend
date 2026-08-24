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
- [x] Done

**Built:**
- Request DTOs under `com.codearena.dto`: [RegisterRequest.java](src/main/java/com/codearena/dto/RegisterRequest.java)
  (`@NotBlank` username, `@Email` email, `@Size(min=6)` password), [LoginRequest.java](src/main/java/com/codearena/dto/LoginRequest.java),
  [ProblemRequest.java](src/main/java/com/codearena/dto/ProblemRequest.java), [ContestRequest.java](src/main/java/com/codearena/dto/ContestRequest.java),
  [SubmissionRequest.java](src/main/java/com/codearena/dto/SubmissionRequest.java).
- Response DTOs: [AuthResponse.java](src/main/java/com/codearena/dto/AuthResponse.java) (token),
  [UserProfileResponse.java](src/main/java/com/codearena/dto/UserProfileResponse.java)
  (username, rating, problemsSolved, contestsJoined),
  [LeaderboardEntryResponse.java](src/main/java/com/codearena/dto/LeaderboardEntryResponse.java) (rank, username, score).
- Custom cross-field validation: [EndTimeAfterStartTime.java](src/main/java/com/codearena/dto/EndTimeAfterStartTime.java)
  (class-level constraint annotation) + [EndTimeAfterStartTimeValidator.java](src/main/java/com/codearena/dto/EndTimeAfterStartTimeValidator.java),
  applied to `ContestRequest` so `end_time` must be after `start_time`.
- Verified: app boots clean with the new DTOs and the custom validator on the classpath — no
  bean-validation wiring errors.

**Decisions/deviations:**
- `SubmissionRequest` includes a `status` field the caller supplies directly. Per CLAUDE.md, v1
  doesn't execute submitted code — there's no judge to derive ACCEPTED/WRONG_ANSWER/etc., so the
  guide's own scoring model (Phase 11: score depends on `status`) only works if the request
  carries it.
- Only the DTOs the guide's Phase 4 prompt names were built. Response DTOs for returning
  Problem/Contest/Submission data (needed once Phases 8/9/11 add GET endpoints, per the
  "entities never returned directly" rule) are intentionally deferred to those phases, where
  the exact response shape will be driven by what each endpoint actually needs.

**Next:** Phase 5 — Spring Security + JWT (also fixes the two carried-over Phase 1 items above).

---

## Phase 5: Security Layer (Spring Security + JWT)
- [x] Done

**Carried over from Phase 1 — both rectified:**
- Real `SecurityConfig` replaces the permit-all placeholder: public `/api/auth/**`,
  `/api/health`, Swagger paths; everything else authenticated; admin-only write access on
  problems/contests. ✅
- `application.properties` no longer has any DB credential in it — real local credentials
  moved to gitignored [application-local.properties](src/main/resources/application-local.properties)
  (`spring.profiles.active=local`), with a tracked
  [application-local.properties.example](src/main/resources/application-local.properties.example)
  template for anyone else cloning the repo. ✅

**Built:**
- [JwtService.java](src/main/java/com/codearena/security/JwtService.java) — generates/validates
  HS256 tokens (`jjwt` 0.12.6), 24h expiry, secret from `jwt.secret` in `application.properties`
  (a placeholder value per the guide — safe to commit, unlike the DB password).
- [CustomUserDetailsService.java](src/main/java/com/codearena/security/CustomUserDetailsService.java) —
  loads a `User` by email (login is by email, see `LoginRequest`) via `UserRepository`.
- [JwtAuthenticationFilter.java](src/main/java/com/codearena/security/JwtAuthenticationFilter.java) —
  `OncePerRequestFilter`; reads `Authorization: Bearer <token>`, validates it, sets the
  `SecurityContext`. Malformed/expired tokens are caught and treated as "no auth" rather than
  bubbling up as a 500.
- [SecurityConfig.java](src/main/java/com/codearena/security/SecurityConfig.java) — stateless
  sessions, BCrypt `PasswordEncoder`, `DaoAuthenticationProvider`, explicit `401` entry point
  (Spring Security's undeclared fallback is 403, which fails the guide's own verify step),
  and the authorization rules: public paths, `POST/PUT/DELETE /api/problems/**` and
  `POST/PUT /api/contests/**` admin-only, `POST /api/contests/*/join` any authenticated user
  (carved out ahead of the general contests admin rule — Phase 10's join endpoint is a regular
  user action, not admin management), everything else authenticated.
- Tests: [JwtServiceTest.java](src/test/java/com/codearena/security/JwtServiceTest.java) (unit —
  generate/validate round trip, rejects a token issued to someone else) and
  [JwtAuthenticationIntegrationTest.java](src/test/java/com/codearena/security/JwtAuthenticationIntegrationTest.java)
  (`@SpringBootTest` + MockMvc — no token → 401; a real user's valid token passes the security
  chain and reaches Spring MVC's dispatcher, which 404s since no controller is mapped to that
  path yet). Both pass; the integration test's inserted user rolls back via `@Transactional`
  (verified 0 rows in `users` after the run).

**Decisions/deviations:**
- The guide's own verify checklist item "a token from a later login unlocks it" can't be
  tested literally yet — `/api/auth/login` doesn't exist until Phase 6. Verified the
  equivalent instead: a token minted the same way `JwtService` will mint it for a real DB
  user passes the full filter chain. Full end-to-end (real `POST /api/auth/login` response
  token) gets exercised in Phase 6.
- Hit and fixed one real bug while verifying: Spring Security's undeclared fallback for an
  unauthenticated request is `403 Forbidden`, not `401` — added an explicit
  `HttpStatusEntryPoint(UNAUTHORIZED)` so the guide's verify step actually holds.

**Next:** Phase 6 — register/login endpoints (AuthController, AuthService) using the
security pieces built here.

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
