# CodeArena — Step-by-Step Build Guide

Built from your project documentation, broken into 14 phases. Each phase gives you a **ready-to-paste prompt** (written using the ART framework — Act as / Request / Terms) for whatever AI coding assistant you're using, plus a short checklist to confirm it actually works before you move on.

**How to use this:**
1. Paste one phase's prompt into your AI coding tool (Claude Code is a strong fit here since it can create/edit files and run `mvn` directly in your project).
2. Run the verify checklist for that phase.
3. Commit (see the commit messages from your doc's §47 — they line up with these phases).
4. Move to the next phase. Don't skip ahead — later prompts assume earlier layers exist.

Rough pacing against your 30-day scope: Phases 1–4 (~days 1–5, foundation), Phases 5–7 (~days 6–10, security/auth/errors), Phases 8–11 (~days 11–20, core features), Phases 12–14 (~days 21–30, leaderboard/docs/polish).

---

## Phase 1: Project Setup & Skeleton

**Goal:** A bootable Spring Boot app with the right dependencies and package layout.

**Prompt:**
```
Act as: A senior Java backend engineer who sets up production-grade Spring Boot projects.

Request: Initialize a Spring Boot 3.x project called CodeArena using Java 21 and Maven,
with a layered package structure under com.codearena, connected to a local MySQL database.

Terms:
- Dependencies: Spring Web, Spring Data JPA, MySQL Driver, Spring Security, Validation,
  Lombok, springdoc-openapi-starter-webmvc-ui (Swagger)
- Create empty packages: controller, service, repository, entity, dto, security, exception
- Configure application.properties: MySQL connection (db name codearena_db),
  spring.jpa.hibernate.ddl-auto=update, server.port=8080
- Add GET /api/health returning {"status": "UP"} so I can confirm the app boots
- Show me the full project structure and every file you create
```

**Verify before moving on:**
- [ ] `mvn spring-boot:run` starts with no errors
- [ ] `GET /api/health` returns 200

---

## Phase 2: Entity Layer & Database Design

**Goal:** All five JPA entities with correct relationships and constraints.

**Prompt:**
```
Act as: A backend engineer designing relational schemas with JPA/Hibernate.

Request: Create the five JPA entities for CodeArena — User, Problem, Contest,
ContestProblem (join table), ContestParticipant, and Submission — with correct
relationships and constraints.

Terms:
- User: id, username (unique), email (unique), password (BCrypt hash),
  role (enum USER/ADMIN), rating (int, default 0), created_at
- Problem: id, title, description, difficulty (enum EASY/MEDIUM/HARD), topic,
  constraints, input_format, output_format, sample_input, sample_output, created_at
- Contest: id, title, description, start_time, end_time, created_at
- ContestProblem: id, contest_id (FK), problem_id (FK), unique(contest_id, problem_id)
- ContestParticipant: id, user_id (FK), contest_id (FK), joined_at,
  unique(user_id, contest_id) to block duplicate joins
- Submission: id, user_id (FK), problem_id (FK), contest_id (FK), language,
  source_code (TEXT), status (enum ACCEPTED/WRONG_ANSWER/COMPILATION_ERROR),
  score, submitted_at
- Use Lombok (@Getter/@Setter/@Builder), correct @ManyToOne mappings, and
  @Table names: users, problems, contests, contest_problems,
  contest_participants, submissions
```

**Verify before moving on:**
- [ ] Tables auto-create in MySQL with correct FKs (`SHOW TABLES;` / `DESCRIBE <table>;`)
- [ ] Unique constraints exist on the pairs listed above

---

## Phase 3: Repository Layer

**Goal:** Data-access methods every later feature will depend on.

**Prompt:**
```
Act as: A Spring Data JPA specialist.

Request: Create repository interfaces for all five entities with the query
methods CodeArena's business logic will need.

Terms:
- UserRepository: findByUsername, findByEmail, existsByUsername, existsByEmail
- ProblemRepository: paginated findAll(Pageable), findByDifficulty, findByTopic
  (or JpaSpecificationExecutor if you prefer dynamic filtering)
- ContestRepository: standard CRUD
- ContestParticipantRepository: existsByUserIdAndContestId, findByContestId
- SubmissionRepository: findByUserId, findByContestIdAndStatus (for leaderboard),
  findByUserIdOrderBySubmittedAtDesc
```

**Verify before moving on:**
- [ ] App still boots with no repository/query errors

---

## Phase 4: DTOs & Validation

**Goal:** Clean request/response contracts so entities never leak to the client.

**Prompt:**
```
Act as: A backend engineer who keeps API contracts clean and entities internal.

Request: Create request and response DTOs for CodeArena's auth, problem, contest,
and submission flows, with validation annotations.

Terms:
- RegisterRequest (username, email, password) — @NotBlank, @Email, @Size(min=6)
- LoginRequest (email, password)
- ProblemRequest, ContestRequest, SubmissionRequest — @NotBlank/@NotNull per field
- ContestRequest: custom validation that end_time is after start_time
- Response DTOs: AuthResponse (token), UserProfileResponse (username, rating,
  problemsSolved, contestsJoined), LeaderboardEntryResponse (rank, username, score)
- Entities must never be returned directly from a controller
```

---

## Phase 5: Security Layer (Spring Security + JWT)

**Goal:** Stateless JWT auth wired into Spring Security.

**Prompt:**
```
Act as: A security-focused Spring engineer.

Request: Implement stateless JWT authentication for CodeArena: SecurityConfig,
JwtService, JwtAuthenticationFilter, and BCrypt password encoding.

Terms:
- Flow: login -> AuthenticationManager validates credentials -> JwtService
  generates token -> client sends "Authorization: Bearer <token>" on future
  requests -> JwtAuthenticationFilter validates it and sets the SecurityContext
- Public (no auth): /api/auth/**, /api/health, Swagger paths
- Everything else requires authentication
- Admin-only (@PreAuthorize or config): POST/PUT/DELETE /api/problems/**,
  POST/PUT /api/contests/**
- JWT secret in application.properties (placeholder value), 24h expiry
```

**Verify before moving on:**
- [ ] Hitting a protected endpoint with no token returns 401
- [ ] A token from a later login unlocks it

---

## Phase 6: Authentication Module

**Goal:** Working register/login endpoints.

**Prompt:**
```
Act as: A backend engineer implementing an auth module end to end.

Request: Implement POST /api/auth/register and POST /api/auth/login using the
entities, DTOs, and security pieces already built.

Terms:
- Register: validate input -> reject if username/email exists (409 Conflict) ->
  hash password with BCrypt -> save user with role USER -> return 201
- Login: validate credentials -> issue JWT -> return {"token": "..."}
- Wrong password/nonexistent email -> 401
```

**Verify before moving on:**
- [ ] Register a user via Postman, then log in and receive a token
- [ ] Duplicate email/username returns 409

---

## Phase 7: Global Exception Handling

**Goal:** Consistent error responses everywhere.

**Prompt:**
```
Act as: A backend engineer standardizing API error handling.

Request: Add centralized exception handling using @RestControllerAdvice.

Terms:
- ResourceNotFoundException -> 404
- DuplicateResourceException -> 409
- UnauthorizedException -> 401/403 as appropriate
- BadRequestException -> 400
- MethodArgumentNotValidException (failed @Valid) -> 400 with field-level messages
- Response shape: {"status": <int>, "message": "<string>"}
```

---

## Phase 8: Problem Management Module

**Goal:** Full CRUD + search/filter/pagination for problems.

**Prompt:**
```
Act as: A backend engineer building a CRUD module with search and pagination.

Request: Implement the Problem module: POST, GET (list), GET by id, PUT, DELETE
at /api/problems, with search, filtering, and pagination.

Terms:
- POST/PUT/DELETE restricted to ROLE_ADMIN; GET open to any authenticated user
- GET /api/problems supports optional ?difficulty=&topic=&page=&size=
- Controllers stay thin (receive, validate, delegate); business logic lives in
  ProblemService
```

**Verify before moving on:**
- [ ] Admin token can create/update/delete a problem; a regular USER token gets 403
- [ ] `GET /api/problems?difficulty=EASY&page=0&size=10` returns filtered, paginated results

---

## Phase 9: Contest Management Module

**Goal:** Contest CRUD, problem association, and server-side state calculation.

**Prompt:**
```
Act as: A backend engineer implementing time-based state logic.

Request: Implement contest management: create/view contests, associate problems
with a contest, and calculate contest state server-side.

Terms:
- POST/GET /api/contests, GET /api/contests/{id} — create is admin-only, view is
  open to authenticated users
- POST /api/contests/{contestId}/problems/{problemId} (admin-only) creates a
  ContestProblem row
- State is computed purely from start_time/end_time vs. server time — never trust
  a client-supplied "now": UPCOMING if now < start_time, ACTIVE if
  start_time <= now < end_time, ENDED if now >= end_time
- GET /api/contests/{id}/status returns {"status": "...", "remainingSeconds": ...}
```

---

## Phase 10: Contest Participation

**Goal:** Users can join contests, with duplicate-join and timing checks.

**Prompt:**
```
Act as: A backend engineer implementing participation business rules.

Request: Implement POST /api/contests/{id}/join.

Terms:
- Before allowing a join, verify in order: user is authenticated, contest exists,
  contest has not ended, user hasn't already joined
- Already joined -> 409 Conflict
- Successful join creates a ContestParticipant row
```

**Verify before moving on:**
- [ ] Joining the same contest twice returns 409
- [ ] Joining an ended contest is rejected

---

## Phase 11: Submission Module + Score Calculation

**Goal:** Users can submit solutions; scores are calculated correctly.

**Prompt:**
```
Act as: A backend engineer implementing submission validation and scoring.

Request: Implement POST /api/submissions and GET /api/submissions/my, plus a
dedicated ScoreService.

Terms:
- Validation chain, in order: user authenticated -> contest exists -> problem
  exists -> user has joined the contest -> contest is ACTIVE -> problem belongs
  to the contest -> create submission
- Version 1 does not execute code (per project scope) — persist the submission
  with its status/score fields as data, no execution engine
- ScoreService: if status is ACCEPTED, score = points for the problem's
  difficulty (EASY=100, MEDIUM=200, HARD=300); otherwise score = 0
- Scoring logic lives in ScoreService, never inline in the controller
- GET /api/submissions/my returns the caller's own submissions only
  (problem, contest, language, status, score, submittedAt)
```

**Verify before moving on:**
- [ ] Submitting to a contest you haven't joined is rejected
- [ ] An ACCEPTED submission for a MEDIUM problem scores 200; a WRONG_ANSWER scores 0

---

## Phase 12: Leaderboard, Profile & Rating

**Goal:** Rankings, a profile endpoint, and the simple rating bump.

**Prompt:**
```
Act as: A backend engineer implementing aggregation and ranking logic.

Request: Implement GET /api/contests/{id}/leaderboard, GET /api/users/me, and a
simple rating update on contest participation.

Terms:
- LeaderboardService: get contest participants -> get their accepted submissions
  for that contest -> sum score per user -> sort descending -> assign rank
- GET /api/users/me returns username, rating, problemsSolved, contestsJoined —
  computed from existing submission/participant data, not stored redundantly
- Rating: +10 for participating in a contest, +50 additional if the user
  finishes top 3 on that contest's leaderboard
```

**Verify before moving on:**
- [ ] Leaderboard for a contest with 2+ participants ranks correctly by score
- [ ] `/api/users/me` reflects a real problemsSolved/contestsJoined count after test submissions

---

## Phase 13: API Documentation + Testing

**Goal:** Swagger docs and a Postman collection covering your doc's test list.

**Prompt:**
```
Act as: A backend engineer preparing an API for other developers to consume.

Request: Wire up springdoc-openapi (Swagger UI) documenting every CodeArena
endpoint, and generate a Postman collection covering the test cases below.

Terms:
- Swagger grouped by tag: Authentication, Problems, Contests, Submissions,
  Leaderboard, Users — each endpoint documents method, request body, auth
  requirement, success response, and error responses
- Postman collection (codearena.postman_collection.json) with example requests
  for: successful registration, duplicate email, successful/invalid login,
  admin problem CRUD, non-admin blocked from problem CRUD, search/filter/
  pagination, contest create/view/add-problem, join + duplicate join, join
  before/during/after contest window, valid/invalid submissions,
  non-participant submission blocked, leaderboard correctness with multiple
  participants
```

---

## Phase 14: Git Hygiene & Resume Polish

**Goal:** A repo and story that actually reads well to a hiring manager.

This one's not a code prompt — it's a checklist:

- [ ] Commit history reads like your doc's §47 list (one meaningful commit per phase above), not "final", "final2", "updated"
- [ ] README includes: what the project does, the tech stack, the architecture diagram (Controller → Service → Repository → JPA → MySQL), setup instructions, and the full API list
- [ ] README has a short "what I'd do at scale" section — mention the real code-execution engine, Redis caching, and React frontend as deliberate v2 scope (shows judgment, not just execution)
- [ ] Add 2–3 resume bullets you can lift directly, e.g.:
  - *"Designed and built a 20+ endpoint REST API for a coding-contest platform using Spring Boot, MySQL, and JWT-based authentication with role-based access control."*
  - *"Implemented server-side contest state management, duplicate-participation prevention, and a scoring/leaderboard engine backed by a layered Controller-Service-Repository architecture."*
  - *"Documented and tested the full API surface with Swagger/OpenAPI and a Postman regression suite covering auth, CRUD, and business-rule edge cases."*

---

## Reference: full endpoint list (for your own tracking)

```
Auth:          POST /api/auth/register, POST /api/auth/login
Problems:      POST/GET /api/problems, GET/PUT/DELETE /api/problems/{id}
Contests:      POST/GET /api/contests, GET /api/contests/{id}
               POST /api/contests/{contestId}/problems/{problemId}
               POST /api/contests/{id}/join
               GET  /api/contests/{id}/status
               GET  /api/contests/{id}/leaderboard
Submissions:   POST /api/submissions, GET /api/submissions/my
Users:         GET /api/users/me
```
