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
- [x] Done

**Built:**
- [AuthController.java](src/main/java/com/codearena/controller/AuthController.java) —
  `POST /api/auth/register` (201, no body), `POST /api/auth/login` (200, `AuthResponse`).
- [AuthService.java](src/main/java/com/codearena/service/AuthService.java) — register checks
  `existsByUsername`/`existsByEmail` before saving (BCrypt-hashed password, role `USER`); login
  goes through the real `AuthenticationManager` (→ `DaoAuthenticationProvider` →
  `CustomUserDetailsService` + `PasswordEncoder`, all from Phase 5) exactly as the guide's
  Phase 5 flow describes, then `JwtService` mints the token.
- Minimal exception handling to make 409/401 actually work now: [DuplicateResourceException.java](src/main/java/com/codearena/exception/DuplicateResourceException.java),
  [UnauthorizedException.java](src/main/java/com/codearena/exception/UnauthorizedException.java),
  [ErrorResponse.java](src/main/java/com/codearena/exception/ErrorResponse.java) (`{"status":.., "message":..}`,
  the exact shape Phase 7 specs), [GlobalExceptionHandler.java](src/main/java/com/codearena/exception/GlobalExceptionHandler.java)
  (`@RestControllerAdvice`, only these two exceptions for now).
- Verified live end-to-end with curl: register → 201; login → 200 + real JWT; that JWT on a
  protected path → 404 (not 401 — proves it passed the full security chain, since no
  controller exists at that path yet); no token → 401; wrong password → 401; nonexistent
  email → 401 (same message as wrong password, no user-enumeration leak); duplicate username
  → 409; duplicate email → 409. This also closes out the Phase 5 verify item that couldn't be
  tested literally back then ("a token from a later login unlocks it").
- Full `mvnw test` suite still green after these changes (4 tests total).

**Decisions/deviations:**
- Found and fixed a real bug while verifying live (not caught by the Phase 5 MockMvc test,
  which doesn't reproduce it): a valid, correctly-authenticated request to a path with no
  controller was returning **401** instead of 404. Root cause — Spring Boot internally
  forwards unmapped requests to `/error`, and that forward re-enters the security filter
  chain as a *new*, anonymous request; since `/error` wasn't in the public path list, it got
  rejected by `anyRequest().authenticated()`, masking the real 404 with a misleading 401.
  Fixed by adding `/error` to `SecurityConfig`'s public paths (the standard Spring
  Boot + Spring Security fix for this).
- Register returns 201 with no body — the guide only specifies "return 201," and no
  `UserResponse` DTO was defined in Phase 4 for this, so no response shape was invented.
- Built only the two exceptions Phase 6 actually throws. `ResourceNotFoundException`,
  `BadRequestException`, and `@Valid`/`MethodArgumentNotValidException` handling are still
  Phase 7's job.

**Next:** Phase 7 — expand `GlobalExceptionHandler` with the rest of the guide's exception
mapping (`ResourceNotFoundException` → 404, `BadRequestException` → 400,
`MethodArgumentNotValidException` → 400 with field errors).

---

## Phase 7: Global Exception Handling
- [x] Done

**Built:**
- New exceptions: [ResourceNotFoundException.java](src/main/java/com/codearena/exception/ResourceNotFoundException.java) (404),
  [BadRequestException.java](src/main/java/com/codearena/exception/BadRequestException.java) (400).
- [GlobalExceptionHandler.java](src/main/java/com/codearena/exception/GlobalExceptionHandler.java)
  expanded with all five guide mappings, plus `MethodArgumentNotValidException` → 400, joining
  every field error (and class-level errors like `EndTimeAfterStartTime`) into one `message`
  string as `"field: reason; field: reason"`, since the response shape is a flat
  `{"status","message"}` pair, not a field-map.
- [RestAuthenticationEntryPoint.java](src/main/java/com/codearena/security/RestAuthenticationEntryPoint.java)
  and [RestAccessDeniedHandler.java](src/main/java/com/codearena/security/RestAccessDeniedHandler.java) —
  replace the bare `HttpStatusEntryPoint` from Phase 5 so 401 (no/bad token) and 403
  (authenticated but wrong role) also return the same `{"status","message"}` JSON body.
  These live in `security/`, not `exception/`, because Spring Security rejects those requests
  at the filter level, before `@RestControllerAdvice` ever sees them.
- Tests: [GlobalExceptionHandlerTest.java](src/test/java/com/codearena/exception/GlobalExceptionHandlerTest.java)
  (each handler → correct status + body) and [ContestRequestValidationTest.java](src/test/java/com/codearena/dto/ContestRequestValidationTest.java)
  (exercises `EndTimeAfterStartTime` directly via `Validator`, since no controller uses
  `ContestRequest` yet — that's Phase 9). 10 tests total, all green.
- Verified live: blank username + malformed email on register → 400 with both field errors
  joined in one message; no token on a protected path → 401 JSON body (previously empty);
  non-admin token on an admin-only endpoint → 403 JSON body (previously Spring's default);
  existing 404/409 paths and `/api/health` unaffected.

**Decisions/deviations:**
- Interpreted "UnauthorizedException → 401/403 as appropriate" as: `UnauthorizedException`
  itself stays 401 (bad/missing credentials, thrown by `AuthService`), and Spring Security's
  own `AccessDeniedException` (insufficient role) gets its own handler mapped to 403 — same
  response shape, different mechanism, since one is an app-level exception and the other is a
  security-filter-level rejection that `@RestControllerAdvice` can't intercept.

**Next:** Phase 8 — Problem Management module (CRUD + search/filter/pagination), the first
real use of `ProblemRepository`'s `JpaSpecificationExecutor` from Phase 3 and `ProblemRequest`
from Phase 4.

---

## Phase 8: Problem Management Module
- [x] Done

**Built:**
- [ProblemResponse.java](src/main/java/com/codearena/dto/ProblemResponse.java) — the response
  DTO deferred from Phase 4, now that the shape is actually needed.
- [ProblemService.java](src/main/java/com/codearena/service/ProblemService.java) — CRUD plus
  `search(difficulty, topic, pageable)` built on `ProblemRepository`'s `JpaSpecificationExecutor`
  from Phase 3, with both filters independently optional.
- [ProblemController.java](src/main/java/com/codearena/controller/ProblemController.java) — thin,
  delegates everything to the service. `POST/GET/GET-by-id/PUT/DELETE /api/problems`. No
  SecurityConfig changes needed — Phase 5 already restricted POST/PUT/DELETE to `ROLE_ADMIN`
  and left GET open to any authenticated user.
- Verified live end-to-end: admin creates/updates/deletes a problem (201/200/204); a regular
  user's token gets 403 on all three write operations; no token gets 401; GET by id 200, GET a
  nonexistent id 404; `GET /api/problems?difficulty=EASY&page=0&size=10` returns correctly
  filtered, paginated results (`content`, `totalElements`, `totalPages`, etc.).
- Full `mvnw test` suite still green (10 tests, no regressions).

**Decisions/deviations:**
- No admin-creation endpoint exists (the guide doesn't specify one) — promoted a test user to
  `ADMIN` directly via SQL for verification, the same bootstrap step any real deployment of
  this app would need for its first admin.
- Cleaned up leftover manual-test rows from the Phase 2 verification session (a `Test Problem`
  / `Test Contest` pair with a `contest_problems` link) that had been blocking a delete via FK
  — should have been removed back in Phase 2 but wasn't.

**Next:** Phase 9 — Contest Management module (create/view contests, associate problems,
server-side UPCOMING/ACTIVE/ENDED state).

---

## Phase 9: Contest Management Module
- [x] Done

**Built:**
- [ContestResponse.java](src/main/java/com/codearena/dto/ContestResponse.java) and
  [ContestStatusResponse.java](src/main/java/com/codearena/dto/ContestStatusResponse.java) —
  response DTOs (Phase 9 needed both; the guide's Phase 4 list didn't include them, same
  deferred-DTO pattern as `ProblemResponse` in Phase 8).
- [ContestStatus.java](src/main/java/com/codearena/entity/ContestStatus.java) (`UPCOMING`/
  `ACTIVE`/`ENDED`) and a `getStatus()` computed method added to
  [Contest.java](src/main/java/com/codearena/entity/Contest.java) — pure function of
  `start_time`/`end_time` vs. `LocalDateTime.now()`, never a client-supplied "now". Lives on
  the entity (not `ContestService`) because Phase 10's join-timing check and Phase 11's
  "contest is ACTIVE" check both need the exact same computation.
- [ContestService.java](src/main/java/com/codearena/service/ContestService.java) — create,
  list, get-by-id, associate a problem with a contest, and the status/remaining-seconds
  calculation.
- [ContestController.java](src/main/java/com/codearena/controller/ContestController.java) —
  `POST/GET /api/contests`, `GET /api/contests/{id}`, `POST /api/contests/{contestId}/problems/{problemId}`,
  `GET /api/contests/{id}/status`. No `SecurityConfig` changes needed — the admin-only POST
  rules and the `/join` carve-out were already in place from Phase 5.
- [ContestTest.java](src/test/java/com/codearena/entity/ContestTest.java) — unit tests for the
  three state boundaries, including the exact `now == end_time` edge (guide spec: `ACTIVE` is
  `start <= now < end`, so `now` at or past `end_time` must be `ENDED`).
- Verified live end-to-end: non-admin blocked from contest creation (403); end-before-start
  rejected (400, reusing the `EndTimeAfterStartTime` validator from Phase 4); contests created
  in all three time windows report the correct status and a sane `remainingSeconds`; problem
  association succeeds (201), a duplicate is rejected (409), a non-admin is blocked (403), and
  both a missing contest and a missing problem 404 correctly; contest list/detail open to any
  authenticated user. Full `mvnw test` suite green (14 tests, no regressions).

**Decisions/deviations:**
- Built `ContestProblemRepository` — Phase 3's repository list never actually included one for
  the `ContestProblem` join entity (only `ContestParticipantRepository` was specified), but
  Phase 9's own requirement ("`POST .../problems/{problemId}` creates a `ContestProblem` row")
  can't be done without it. Same kind of gap-fill as `ContestConfig`'s temporary permit-all in
  Phase 1 or `ProblemResponse` deferred to Phase 8 — a genuine hole in an earlier phase, filled
  when the later phase that needs it actually arrives.
- `GET /api/contests` returns a plain `List`, not paginated — the guide's Phase 9 terms don't
  ask for pagination here (unlike Phase 8's problem list), and `ContestRepository` was
  specified in Phase 3 as "standard CRUD" only.
- Caught a real testing mistake during live verification, not a code bug: an early manual test
  used UTC (`date -u`) for contest start/end times while the server's `LocalDateTime.now()`
  runs in local (IST) time, so a contest meant to be "active" was actually computed as `ENDED`
  — correctly, since `LocalDateTime` carries no zone info and the guide's spec compares wall-clock
  values directly. Redid the test data using the server's local time; no code change was needed.

**Next:** Phase 10 — Contest Participation (`POST /api/contests/{id}/join`, using
`ContestParticipantRepository` from Phase 3 and `Contest.getStatus()` from this phase).

---

## Phase 10: Contest Participation
- [x] Done

**Built:**
- [ContestParticipantService.java](src/main/java/com/codearena/service/ContestParticipantService.java) —
  the guide's exact ordered check: authenticated (already enforced by `SecurityConfig`) →
  contest exists (404) → contest has not ended (400, via `Contest.getStatus()` from Phase 9) →
  not already joined (409, via `ContestParticipantRepository.existsByUserIdAndContestId` from
  Phase 3) → save the row.
- `POST /api/contests/{id}/join` added to [ContestController.java](src/main/java/com/codearena/controller/ContestController.java),
  resolving the caller via `@AuthenticationPrincipal UserDetails` (username = email, per
  `CustomUserDetailsService` from Phase 5) rather than trusting any client-supplied identity.
- [ContestParticipantServiceTest.java](src/test/java/com/codearena/service/ContestParticipantServiceTest.java) —
  Mockito unit tests for all four branches (not found, ended, duplicate, success) plus an
  explicit case proving an `UPCOMING` contest is joinable (guide only forbids `ENDED`).
- Verified live: no token → 401; join an `ACTIVE` contest → 201 and a real
  `contest_participants` row; joining again → 409; joining an `ENDED` contest → 400; joining a
  nonexistent contest → 404. Full `mvnw test` suite green (19 tests, no regressions).

**Decisions/deviations:**
- "Contest has not ended" maps to `BadRequestException` (400) — the guide doesn't state a
  status code for this check, and 400 fits the existing exception vocabulary better than
  inventing a new one for a single case.
- UPCOMING contests are joinable, only ENDED is rejected — the guide's ordering only says
  "contest has not ended," so pre-registering for a contest that hasn't started is allowed by
  design, not an oversight.

**Next:** Phase 11 — Submission module + ScoreService (`POST /api/submissions`,
`GET /api/submissions/my`), using `SubmissionRepository` from Phase 3 and the same
`Contest.getStatus()` for the "contest is ACTIVE" check.

---

## Phase 11: Submission Module + Score Calculation
- [x] Done

**Built:**
- [ScoreService.java](src/main/java/com/codearena/service/ScoreService.java) — dedicated, as
  the guide requires: ACCEPTED scores 100/200/300 by difficulty, anything else scores 0.
- [SubmissionResponse.java](src/main/java/com/codearena/dto/SubmissionResponse.java) — the
  guide asks for "problem, contest, language, status, score, submittedAt"; interpreted
  "problem"/"contest" as id+title pairs (not the full `ProblemResponse`/`ContestResponse`),
  since a submission list doesn't need full problem descriptions.
- [SubmissionService.java](src/main/java/com/codearena/service/SubmissionService.java) — the
  guide's exact ordered chain: authenticated (`SecurityConfig`) → contest exists → problem
  exists → user has joined (`ContestParticipantRepository`, Phase 3) → contest is `ACTIVE`
  (`Contest.getStatus()`, Phase 9) → problem belongs to the contest
  (`ContestProblemRepository`, Phase 9) → create, scored via `ScoreService`.
  `getMySubmissions` uses `findByUserIdOrderBySubmittedAtDesc` from Phase 3.
- [SubmissionController.java](src/main/java/com/codearena/controller/SubmissionController.java) —
  `POST /api/submissions`, `GET /api/submissions/my`. Caller resolved from the JWT principal,
  same pattern as Phase 10's join endpoint. No `SecurityConfig` changes needed — falls to
  `anyRequest().authenticated()`, no admin restriction.
- Tests: [ScoreServiceTest.java](src/test/java/com/codearena/service/ScoreServiceTest.java)
  (all difficulty × status combinations) and
  [SubmissionServiceTest.java](src/test/java/com/codearena/service/SubmissionServiceTest.java)
  (Mockito — every branch of the validation chain, plus both of the guide's explicit verify
  cases: ACCEPTED/MEDIUM → 200, WRONG_ANSWER → 0).
- Verified live end-to-end: submit before joining → 400; problem not in the contest → 400;
  missing contest/problem → 404 each; submit to an `UPCOMING` (not yet `ACTIVE`) contest → 400;
  valid ACCEPTED/MEDIUM submission → 201, score 200; WRONG_ANSWER → 201, score 0;
  `GET /api/submissions/my` returns only the caller's own submissions, newest first; no token
  → 401. Full `mvnw test` suite green (31 tests, no regressions).

**Decisions/deviations:**
- "User has joined" and "problem belongs to the contest" failures map to `BadRequestException`
  (400) — same reasoning as Phase 10's "contest has ended" case: the guide doesn't specify a
  status code for these, and 400 fits the existing exception vocabulary.

**Next:** Phase 12 — Leaderboard, profile & rating (`GET /api/contests/{id}/leaderboard`,
`GET /api/users/me`), built on `SubmissionRepository.findByContestIdAndStatus` from Phase 3
and the participant/submission data this phase now produces.

---

## Phase 12: Leaderboard, Profile & Rating
- [x] Done

**Built:**
- [LeaderboardService.java](src/main/java/com/codearena/service/LeaderboardService.java) — the
  guide's exact pipeline: participants → their accepted submissions for that contest, summed
  per user → sorted descending → sequential rank assigned. When the contest is `ENDED`, also
  applies the rating bump (+10 participating, +50 more for top 3) idempotently.
- [UserService.java](src/main/java/com/codearena/service/UserService.java) — `GET /api/users/me`
  data computed live from submissions/participants, never stored redundantly: `problemsSolved`
  counts *distinct* problems with an ACCEPTED submission (not total accepted submissions),
  `contestsJoined` from `ContestParticipantRepository`. `rating` itself is stored (it's a real
  `User` column since Phase 2, unlike the other two fields), maintained by `LeaderboardService`.
- `GET /api/contests/{id}/leaderboard` on [ContestController.java](src/main/java/com/codearena/controller/ContestController.java);
  new [UserController.java](src/main/java/com/codearena/controller/UserController.java) for
  `GET /api/users/me`.
- Schema addition: `rating_applied` boolean on [ContestParticipant.java](src/main/java/com/codearena/entity/ContestParticipant.java)
  — see deviations below.
- Repository addition: `countByUserId` on [ContestParticipantRepository.java](src/main/java/com/codearena/repository/ContestParticipantRepository.java)
  (Phase 3 gap, same pattern as `ContestProblemRepository` in Phase 9 — needed for
  `contestsJoined` and never specified).
- Tests: [LeaderboardServiceTest.java](src/test/java/com/codearena/service/LeaderboardServiceTest.java)
  (ranking, score-summing across multiple accepted submissions, no rating change while
  `ACTIVE`, correct +10/+60 split on contest end, and — critically — a second `getLeaderboard`
  call does **not** re-apply the bonus) and [UserServiceTest.java](src/test/java/com/codearena/service/UserServiceTest.java)
  (distinct-problem counting).
- Verified live with 4 participants on a real timed contest: leaderboard ranked correctly
  (300/200/200/0, ranks 1–4) while `ACTIVE` with rating still 0; once the contest genuinely
  ended, viewing the leaderboard bumped ranks 1–3 to rating 60 (10+50) and rank 4 to 10
  (participation only); a second view did not double-apply; `/api/users/me` showed real
  `problemsSolved`/`contestsJoined` throughout. Full `mvnw test` suite green (38 tests, no
  regressions).

**Decisions/deviations:**
- Added `ratingApplied` to `ContestParticipant` — without it, every leaderboard view on an
  ended contest would re-credit rating, since there's no other "contest finalized" event in
  this guide (no scheduler, no finalize endpoint). This is the mechanism that makes "a simple
  rating update on contest participation" actually correct rather than just simple.
- Rank ties aren't collapsed — two participants with equal score still get sequential ranks
  (1, 2, 3, ...), not shared ranks. The guide says "sort descending → assign rank" with no
  tie-breaking rule, so this is the literal reading; it also means a tie for 3rd/4th place
  resolves by whichever sort order Java's stable sort preserves (insertion order from
  `findByContestId`), not by e.g. earliest submission time.
- Top-3 rating bonus is based on leaderboard rank, not score — so a participant who joined but
  never submitted can still land in the top 3 (with score 0) if fewer than 3 others
  outscored them, and would still get the bonus. This follows from "top 3 on that contest's
  leaderboard" literally; the guide gives no minimum-score qualifier.

**Next:** Phase 13 — API documentation (Swagger/OpenAPI, already partially wired via
`springdoc-openapi` since Phase 1) and a Postman regression collection covering the full test
list from the guide.

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
