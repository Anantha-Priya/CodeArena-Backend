# CodeArena — Frontend Build Guide

Companion to [CodeArena_Build_Guide.md](CodeArena_Build_Guide.md). That guide builds the
Spring Boot REST API in 14 phases; this one builds a React frontend on top of it, in the
same style — a ready-to-paste ART-framework prompt per phase, plus a short verify checklist.

**Important — read before starting:** This guide assumes the backend's 14 phases are
already fully built and working (every endpoint in the Reference list at the bottom
responds correctly). The frontend was explicitly called out as v2 scope in the backend
guide's own Phase 14 checklist ("mention ... a React frontend as deliberate v2 scope"),
so this is that v2 — a separate build, started only once the API is real and running.

**Tech choice:** React (matching the "React frontend" already named in the backend guide),
with Vite + TypeScript, `react-router-dom` for routing, and a thin fetch/Axios wrapper for
the API — no heavier framework needed for a project this size. Swap freely if you'd rather
use plain JS, Next.js, or another stack; the phase breakdown and prompts still apply.

**How to use this:**
1. Paste one phase's prompt into your AI coding tool, in a new `codearena-frontend`
   project (kept separate from the Spring Boot repo — two `git` histories, two deploys).
2. Keep the backend running (`.\mvnw.cmd spring-boot:run` in the CodeArena folder) while
   you build and test each frontend phase against it.
3. Run the verify checklist for that phase.
4. Commit, then move to the next phase.

Rough pacing: Phases 1–3 (foundation: setup, auth, shell), Phases 4–6 (problems),
Phases 7–10 (contests, joining, submissions), Phase 11–12 (leaderboard, profile),
Phase 13–14 (polish, deployment).

---

## Phase 1: Project Setup & Skeleton

**Goal:** A bootable React app wired to talk to the CodeArena API.

**Prompt:**
```
Act as: A senior frontend engineer who sets up production-grade React + TypeScript projects.

Request: Initialize a React + TypeScript project called codearena-frontend using Vite,
with routing and an API client layer pointed at the CodeArena Spring Boot backend.

Terms:
- Tooling: Vite, React 18+, TypeScript, react-router-dom
- Folder structure: src/pages, src/components, src/api, src/hooks, src/context, src/types
- Create a typed API client (src/api/client.ts) using fetch or Axios, base URL read from
  an env var (VITE_API_BASE_URL, default http://localhost:8080)
- Add a Home page that calls GET /api/health and displays "Backend: UP" or "Backend:
  unreachable" so I can confirm the two projects can talk to each other
- Show me the full project structure and every file you create
```

**Verify before moving on:**
- [ ] `npm run dev` starts with no errors
- [ ] With the backend running, the Home page shows "Backend: UP"
- [ ] With the backend stopped, the Home page shows the unreachable state instead of crashing

---

## Phase 2: Auth Pages & Token Handling

**Goal:** Working register/login screens that store and attach the JWT.

**Prompt:**
```
Act as: A frontend engineer implementing JWT-based auth flows.

Request: Build Register and Login pages that call the CodeArena auth endpoints, store the
returned token, and attach it to every future API request.

Terms:
- POST /api/auth/register — body {username, email, password}; on success (201) redirect
  to Login with a success message; on 409 (duplicate) show an inline field error
- POST /api/auth/login — body {email, password}; response is {"token": "..."}; on 401
  show "invalid email or password"
- Store the token (localStorage is fine for this project's scope) and create an
  AuthContext/useAuth hook exposing { token, isAuthenticated, login, logout }
- API client automatically adds "Authorization: Bearer <token>" to every request once
  logged in
- On a 401 response from ANY endpoint (token expired/invalid), clear the token and
  redirect to Login
```

**Verify before moving on:**
- [ ] Registering a new user then logging in lands you in the authenticated app
- [ ] A wrong password shows an error, doesn't crash, doesn't store a token
- [ ] Refreshing the page keeps you logged in (token persisted, not lost)

---

## Phase 3: Protected Routing & App Shell

**Goal:** A real layout with role-aware navigation and route guards.

**Prompt:**
```
Act as: A frontend engineer implementing route protection and role-based UI.

Request: Add a persistent app shell (navbar + content area) and protect all non-auth
routes behind login, with admin-only links hidden from regular users.

Terms:
- On login, call GET /api/users/me to get the current user's role, username, and rating;
  store it in AuthContext alongside the token
- ProtectedRoute wrapper redirects unauthenticated users to /login
- Navbar shows: Problems, Contests, My Submissions, Profile (all users) and Admin: Problems,
  Admin: Contests (ROLE_ADMIN only), plus username/rating and a Logout button
- Logout clears the token/user and redirects to /login
```

**Verify before moving on:**
- [ ] Logged out, visiting any protected URL directly redirects to /login
- [ ] A regular USER doesn't see the Admin nav links; logging in as an admin account does

---

## Phase 4: Problems List Page

**Goal:** Browse, filter, and page through the problem set.

**Prompt:**
```
Act as: A frontend engineer building a filterable, paginated data view.

Request: Build a Problems list page backed by GET /api/problems.

Terms:
- Difficulty filter (EASY/MEDIUM/HARD dropdown) and topic filter (text input), both
  optional, mapped to ?difficulty=&topic=
- Pagination controls mapped to ?page=&size= (backend returns a Spring Page object —
  read totalPages/totalElements from it, don't just guess from array length)
- Each row/card shows title, difficulty (color-coded badge), topic; clicking navigates to
  the problem detail page
- Loading skeleton while fetching, empty state when a filter returns nothing, error state
  if the request fails
```

**Verify before moving on:**
- [ ] Filtering by difficulty and topic together narrows results correctly
- [ ] Page 2+ loads different problems than page 1 (not stuck re-showing page 1)

---

## Phase 5: Problem Detail Page

**Goal:** Full problem statement view.

**Prompt:**
```
Act as: A frontend engineer building a content detail view.

Request: Build a Problem Detail page backed by GET /api/problems/{id}.

Terms:
- Render title, difficulty badge, topic, description, constraints, input_format,
  output_format, and sample_input/sample_output in a readable, monospace-for-code layout
- 404 from the backend (bad id) shows a clear "problem not found" state, not a blank page
- No submit-here action yet — that's tied to a contest and comes in Phase 10
```

**Verify before moving on:**
- [ ] A real problem id renders all its fields correctly
- [ ] An invalid id shows the not-found state instead of an error boundary crash

---

## Phase 6: Admin Problem Management

**Goal:** Admins can create, edit, and delete problems from the UI.

**Prompt:**
```
Act as: A frontend engineer building an admin CRUD interface.

Request: Build an Admin Problems page with create/edit/delete, restricted to ROLE_ADMIN.

Terms:
- Route-guard this page: a non-admin who navigates here directly is redirected away, not
  just hidden from the nav
- Create/edit form fields match ProblemRequest: title, description, difficulty, topic,
  constraints, input_format, output_format, sample_input, sample_output — client-side
  required-field validation mirroring the backend's @NotBlank/@NotNull rules
- Delete asks for confirmation before calling DELETE /api/problems/{id}
- A non-admin token hitting these calls gets 403 from the backend — surface that as a
  clear "not authorized" message if it's ever reached some other way
```

**Verify before moving on:**
- [ ] Admin can create a problem and immediately see it in the Phase 4 list
- [ ] Admin can edit and delete an existing problem
- [ ] Non-admin cannot reach this page via direct URL

---

## Phase 7: Contests List & Detail

**Goal:** Browse contests with live, server-driven status.

**Prompt:**
```
Act as: A frontend engineer implementing server-authoritative time-based state in the UI.

Request: Build a Contests list page (GET /api/contests) and a Contest Detail page
(GET /api/contests/{id}), both showing live contest status.

Terms:
- Status badge and countdown come from GET /api/contests/{id}/status
  ({"status": "...", "remainingSeconds": ...}) — poll it every 5-10 seconds while the page
  is open; never compute UPCOMING/ACTIVE/ENDED from the client's own clock, the backend
  is the source of truth
- Countdown displays as a human-readable timer counting down remainingSeconds
- Detail page lists the contest's associated problems (from the contest object /
  ContestProblem data) with links to each Problem Detail page
```

**Verify before moving on:**
- [ ] An ACTIVE contest's countdown visibly ticks down and matches backend state
- [ ] Refreshing near a contest's end time correctly flips the badge to ENDED

---

## Phase 8: Contest Join Flow

**Goal:** Users can join a contest, with correct handling of every rejection case.

**Prompt:**
```
Act as: A frontend engineer implementing a business-rule-heavy action flow.

Request: Add a Join button to the Contest Detail page, backed by POST /api/contests/{id}/join.

Terms:
- Button only shows "Join Contest" if the user hasn't already joined and the contest
  hasn't ended; shows "Joined" (disabled) if already joined
- 409 response (already joined) — shown as a toast/message, not a hard error page
- Join attempted on an ended contest — backend rejects it; surface the message cleanly
- On success, refetch contest state so the UI reflects joined status immediately
```

**Verify before moving on:**
- [ ] Joining once succeeds and the button updates to "Joined"
- [ ] Trying to join again is blocked in the UI, and even if forced, shows the 409 message
  gracefully instead of crashing

---

## Phase 9: Admin Contest Management

**Goal:** Admins can create contests and attach problems to them.

**Prompt:**
```
Act as: A frontend engineer building an admin workflow with cross-field validation.

Request: Build an Admin Contests page: create a contest, and attach existing problems to it.

Terms:
- Create form fields match ContestRequest: title, description, start_time, end_time —
  client-side validation that end_time is after start_time, mirroring the backend's
  @EndTimeAfterStartTime constraint (don't rely on the backend's 400 alone for UX)
- A problem-attach UI: pick from the existing problem list (reuse Phase 4's fetch),
  call POST /api/contests/{contestId}/problems/{problemId} per selection
- Restricted to ROLE_ADMIN the same way as Phase 6
```

**Verify before moving on:**
- [ ] Submitting end_time before start_time is blocked client-side with a clear message
- [ ] A newly created contest appears in the Phase 7 list; attached problems show on its
  detail page

---

## Phase 10: Submission Form & My Submissions

**Goal:** Submit a solution within a joined, active contest; view submission history.

**Prompt:**
```
Act as: A frontend engineer implementing a form with backend-enforced business rules.

Request: Add a Submit Solution form (from the Problem Detail page, when reached via an
active contest) and a My Submissions page (GET /api/submissions/my).

Terms:
- Submit form fields match SubmissionRequest: problem, contest, language, source_code,
  and status — note v1 has no code execution engine, so status is a manual field the user
  sets (e.g. a dropdown: ACCEPTED / WRONG_ANSWER / COMPILATION_ERROR), not computed
  server-side; label this clearly in the UI so it doesn't read as a real judge
- Handle every backend rejection distinctly: not joined the contest, contest not ACTIVE,
  problem doesn't belong to the contest — each as its own clear message, not a generic error
- My Submissions table: problem, contest, language, status, score, submittedAt, newest first
```

**Verify before moving on:**
- [ ] Submitting to a contest you haven't joined shows the specific rejection reason
- [ ] A successful ACCEPTED submission appears in My Submissions with the correct score

---

## Phase 11: Leaderboard Page

**Goal:** Ranked standings per contest.

**Prompt:**
```
Act as: A frontend engineer building a ranked data view.

Request: Build a Leaderboard page per contest, backed by
GET /api/contests/{id}/leaderboard.

Terms:
- Table columns: rank, username, score, sorted by rank ascending (already sorted by the
  backend — don't re-sort client-side in a way that could disagree with it)
- Visually highlight the logged-in user's own row if present
- Linked from the Contest Detail page
```

**Verify before moving on:**
- [ ] A contest with 2+ participants shows correctly ranked standings
- [ ] Your own row is visually distinct when you're on the board

---

## Phase 12: Profile Page

**Goal:** A simple "me" page.

**Prompt:**
```
Act as: A frontend engineer building a profile summary view.

Request: Build a Profile page backed by GET /api/users/me.

Terms:
- Display username, rating, problemsSolved, contestsJoined as simple stat cards
- No editing in v1 — this endpoint is read-only, so the page is read-only too
```

**Verify before moving on:**
- [ ] Values shown match what you'd expect from your own test submissions/contest joins

---

## Phase 13: Polish — Errors, Loading, Responsiveness

**Goal:** The app feels finished, not just functional.

**Prompt:**
```
Act as: A frontend engineer hardening an app for real use.

Request: Add consistent error handling, loading states, and responsive layout across the
whole app.

Terms:
- Global handling for the backend's standard error shape {"status": <int>, "message":
  "<string>"} — surface message as a toast/banner consistently instead of each page
  reinventing it
- 400 with field-level validation errors (failed @Valid) — map back to the relevant form
  fields where possible
- Loading skeletons instead of blank screens on every data fetch
- Mobile-usable layout for the list/detail/leaderboard pages at minimum
```

---

## Phase 14: Deployment & Resume Polish

**Goal:** A frontend that's actually deployed, and a story that reads well.

This one's not a code prompt — it's a checklist:

- [ ] Production build works (`npm run build`) with `VITE_API_BASE_URL` pointed at wherever
  the backend actually runs in production
- [ ] Deployed somewhere reachable (Vercel/Netlify/static hosting are all fine for this
  scope) — link it from the backend's README
- [ ] Commit history reads like real incremental work (one meaningful commit per phase
  above), not "final"/"final2"
- [ ] README (or a section in the backend's) covers: what the frontend does, tech stack,
  how it talks to the API, setup instructions, and a link/screenshot of it running
- [ ] Add 1–2 resume bullets you can lift directly, e.g.:
  - *"Built a React + TypeScript frontend for a coding-contest platform, consuming a
    JWT-secured REST API — role-based routing, live server-driven contest state, and
    business-rule-aware forms for a multi-entity submission/leaderboard flow."*

---

## Reference: page/route list (for your own tracking)

```
Public:        /login, /register
Authenticated: /              (Home / backend health)
               /problems, /problems/:id
               /contests, /contests/:id, /contests/:id/leaderboard
               /submissions/my
               /profile
Admin only:    /admin/problems  (create/edit/delete)
               /admin/contests  (create + attach problems)
```
