# Task Board — Technical Requirements

## 1. Document control

| Field | Value |
|---|---|
| Product | Task Board (Android) |
| Source brief | `https://www.markdownpaste.com/document/-live-coding-challenge-android-ai-assisted-1` |
| Brief last modified | 2026-09-08 |
| Retrieved | 2026-09-08 |
| Status | Baselined |
| Companion document | [BACKLOG.md](BACKLOG.md) — epics, stories, tasks, iterations |
| Implementation status | Iterations 0 (foundation) and 1 (read path: E1–E3) delivered; see [../README.md](../README.md) |

Requirement IDs in this document are stable. `BACKLOG.md` references them; do not renumber.

## 2. Product context and objective

A Task Board lets one person track their own tasks. This project builds a vertical slice of it: a
list of tasks, the ability to add, complete and delete them, and a detail screen for editing.

There is no backend. The app talks to a mock network source that the project stands up itself, and
that source behaves like a real remote API — it is slow and it sometimes fails. That is deliberate:
it makes loading and error handling genuine product behaviour rather than a demonstration.

**Delivery constraint.** The brief is a 60-minute live exercise: ~5 minutes to read and plan,
~45 minutes to build, ~10 minutes to walk through the result. This constraint is recorded here
because it drives prioritisation and the cut-line in `BACKLOG.md`. It does not change what a
requirement *is*, so it does not structure this document.

**Quality bar, from the brief.** *"Treat it like the start of a real product you'd hand to a team —
not a throwaway demo."* Read as: boundaries, naming and state handling are graded; feature count is
not. The brief also states that stretch goals are genuinely optional, and that solid core work is
preferred to a rushed pile of half-features.

## 3. Scope

### In scope

- Task list, add, complete, delete, detail/edit.
- A self-built mock network layer with realistic asynchronous behaviour.
- Loading, empty and error states driven by that layer.
- The data layer above the mock source: repositories, caching, use cases.

### Out of scope

| Excluded | Source |
|---|---|
| Real networking | Brief, "Out of scope" |
| Authentication | Brief, "Out of scope" |
| Push notifications | Brief, "Out of scope" |
| On-disk persistence | Brief: "in-memory is fine" |
| Multiple modules | Brief: "aren't needed either" |

The brief adds: *"If you make a deliberate choice in any of these areas, just tell us why."*
Section 4 answers that.

## 4. Deliberate deviations from the brief

| # | Brief says | This project does | Why |
|---|---|---|---|
| D-1 | Single module is fine; multiple modules are not needed | Nine modules: `:app`, two `:feature`, three `:core`, `:data:tasks`, two `:lib` | The boundary rules become compiler-enforced rather than conventions. `:lib:tasks-api` is pure Kotlin JVM, so `import android.*` in the domain does not compile, and a feature cannot reference `DefaultTaskRepository` because `:data:tasks` is not on its classpath. Both were verified by introducing the violation. This is the cost paid for the "start of a real product" framing. |
| D-2 | On-disk persistence not needed; in-memory is fine | In-memory cache (`InMemoryTaskCache`) | Agrees with the brief. Recorded because the cache is a named seam: replacing it with Room touches that file and `DataModule`, and nothing above them. |
| D-3 | Target API 26+ | `minSdk = 26` | The floor the brief allows, chosen so `java.time` is available without desugaring. |
| D-4 | — | Convention plugins in `build-logic/` | Nine modules would otherwise repeat the same `android {}` block nine times. SDK levels live in one place (`AndroidSdk`). |

Each deviation is a talking point for the walkthrough, not a silent choice.

## 5. Domain model

| Type | Fields | Notes |
|---|---|---|
| `Task` | `id: String`, `title: String`, `notes: String?`, `priority: TaskPriority`, `isCompleted: Boolean`, `createdAt: Instant` | Ids are opaque strings assigned by the API; the UI never parses them. |
| `TaskPriority` | `LOW`, `MEDIUM`, `HIGH` | Declared low-to-high so the natural order of the enum is also the sort order. |
| `TaskDraft` | `title`, `notes`, `priority` | The editable half of a `Task`. Create and update take the same payload — this is why one editor screen serves both. |

Defined in `lib/tasks-api/src/main/kotlin/com/rounds/test/to_dolist/tasks/model/`.

**Identity.** `id` is the identity. The list keys rows by it (`LazyColumn` `key = { it.id }`).

**Ordering.** Default order is the order the source returns. Explicit sorting is FR-11.

## 6. Functional requirements

MoSCoW: **M**ust / **S**hould / **C**ould. Core requirements from the brief are Must; stretch goals
are Could.

| ID | Requirement | Pri | Source | Acceptance criteria |
|---|---|---|---|---|
| FR-01 | The user sees a scrollable list of tasks. Each row shows the title, a priority indicator, and a control to mark the task complete. | M | Core 1 | Rows render title, priority and a checkbox. The list scrolls. A very long title does not break the row layout (see the seed data). |
| FR-02 | The user can add a task with a required title, optional notes, and a priority of Low, Medium or High. | M | Core 2 | Save is unavailable while the title is blank. Notes may be empty. Priority defaults to Medium and is changeable. On success the new task appears in the list. |
| FR-03 | The user can toggle a task between complete and incomplete. | M | Core 3 | Toggling from the list updates the row. The change survives a refresh. Completed rows are visually distinct. |
| FR-04 | The user can delete a task. | M | Core 3 | The task disappears from the list and is gone after a refresh. |
| FR-05 | Tapping a task opens a detail screen where its fields can be viewed and edited. | M | Core 4 | The screen opens seeded with the current values. Edits are persisted on save and reflected in the list. |
| FR-06 | The app reads and writes through a self-built mock network source rather than local state. | M | Core 5 | Every read and write goes through `TaskApi`. No screen holds task data that did not come from it. |
| FR-07 | The list handles a loading state. | M | Core 6 | A progress indicator is shown while the first load is in flight and no cached content exists. |
| FR-08 | The list handles an empty state. | M | Core 6 | When the source returns no tasks and no error is present, an empty message is shown instead of a blank screen. |
| FR-09 | The list handles an error state and offers recovery. | M | Core 6 | When a load fails with nothing cached, a typed message and a Retry control are shown. Retry re-issues the load. |
| FR-10 | The user can search or filter the list by title. | C | Stretch | Typing narrows the list to matching titles. Clearing restores it. Empty results show a message distinct from "no tasks at all". |
| FR-11 | The user can sort the list by priority or by completion status. | C | Stretch | A control switches the ordering; the chosen order persists while the screen is open. |
| FR-12 | The user can undo a delete. | C | Stretch | A snackbar with an Undo action appears after a delete. Acting on it restores the task. |
| FR-13 | Tasks carry a due date, displayed with relative formatting. | C | Stretch | The editor sets a due date; the row renders it as "tomorrow", "in 3 days", and so on. |
| FR-14 | The app supports light and dark themes. | C | Stretch | The UI is legible in both. Priority colours stay distinguishable in dark mode. |

## 7. Non-functional requirements

| ID | Requirement | Pri | Verification |
|---|---|---|---|
| NFR-01 | All data access is asynchronous, exposed as suspending functions or `Flow`. | M | `TaskApi` and `TaskRepository` signatures. No blocking call on the main thread. |
| NFR-02 | Reads take 300–800 ms. | M | See §8. Observable as a real loading state on device. |
| NFR-03 | Roughly 15% of loads and saves fail. | M | See §8. |
| NFR-04 | Transport failures never reach the presentation layer. Repositories return `Result` whose failure is always `DataException` carrying a typed `DataError`. | M | No transport type is importable from a `:feature` module — `:data:tasks` is not on its classpath. |
| NFR-05 | Mock latency and failure are deterministic under test. | M | `Random` is injectable and seeded in tests; no test depends on the dice. |
| NFR-06 | The UI survives configuration change with its state intact. | S | Rotate the device on a populated list and on a half-filled editor form. |
| NFR-07 | The UI survives process recreation with its state intact. | C | `adb shell am kill`, or "Don't keep activities". Editor form fields restore. |
| NFR-08 | List rendering is stable and efficient: rows keyed by id, no work per frame. | S | `LazyColumn` uses `key = { it.id }`. |
| NFR-09 | Priority is conveyed by text as well as colour; icon-only controls carry content descriptions. | S | `PriorityIndicator` renders a label; `IconButton`s carry `contentDescription`. Verified with TalkBack or by inspection. |
| NFR-10 | minSdk 26, targetSdk 37, JVM target 17. | M | `build-logic` `AndroidSdk`; `app/build.gradle.kts`. |
| NFR-11 | No `TODO()` is reachable from a user action in a shipped iteration. | M | `grep -rn "TODO(" --include=*.kt` against the paths the iteration touches. |
| NFR-12 | Module dependency rules (§10) are not violated. | M | `./gradlew lint` with `checkDependencies = true`; compile failure on a deliberate violation. |
| NFR-13 | Unit tests cover the mock source, the repository, the use cases and both ViewModels. | M | `./gradlew test` green; every core story has a test task in `BACKLOG.md`. |

## 8. Mock network layer specification

The most tightly specified part of the brief, so it is specified here rather than left to the
implementer.

### Contract

`TaskApi` (`data/tasks/.../api/TaskApi.kt`), already defined:

```kotlin
suspend fun getTasks(): List<TaskDto>
suspend fun getTask(id: String): TaskDto
suspend fun createTask(payload: TaskPayload): TaskDto
suspend fun updateTask(id: String, payload: TaskPayload): TaskDto
suspend fun deleteTask(id: String)
```

It speaks DTOs, never domain models, and it signals failure by throwing, the way a transport does.

There is deliberately no "set completed" operation: five REST-shaped calls are the whole contract, so
`TaskPayload` carries `completed` alongside the editable fields and completing a task is an update
like any other. The repository is what makes that invisible above the data layer — `setCompleted(id,
completed)` reads the task's current fields and sends them back with the flag changed.

### Behaviour

| Aspect | Specification | Source |
|---|---|---|
| Read latency | Uniform random 300–800 ms | Brief: "e.g. 300–800 ms" |
| Failure rate | ~15% on load and on save | Brief: "Fail roughly 15% of the time on load/save" |
| Concurrency | The in-memory store is guarded by a `Mutex`; concurrent callers must not corrupt it | Engineering |
| Ids | Assigned by the source, not the caller | Engineering |
| Timestamps | From the injected `Clock` (`:core:common`), not an inline `Instant.now()` | NFR-05 |
| Determinism | `Random` is injected; tests seed it | NFR-05 |
| Demo switch | The failure rate is configurable so it can be set to 0 for a live demo | Risk R-1 |

> The planning comment in `FakeTaskApi.kt` used to say 200–900 ms and roughly 1-in-7. It predated the
> full brief and was wrong; TB-101 corrected it to the table above. `FakeTaskApi.failureRate` is a
> `var` defaulting to `0.15`, so a demo can set it to `0.0` or `1.0` on cue (risk R-1).

### Error taxonomy

Thrown failures map onto the existing `DataError` (`lib/tasks-api/.../error/DataError.kt`):

| `DataError` | Raised when |
|---|---|
| `Network` | The injected dice fail a call. Retrying immediately is reasonable. |
| `Timeout` | Reserved for a slow-path simulation; same remedy, different wording. |
| `NotFound` | An unknown id — deterministic, never a dice roll. |
| `Conflict` | A write raced another write. The caller should refresh first. |
| `Unknown` | Anything unmapped. |

No strings live in `DataError`. Wording is the job of `:core:ui` (`DataErrorMessages.kt`).

### Seed data

Reproduced exactly from the brief. The fourth row exists to stress row layout and must not be
shortened.

```json
[
  { "title": "Renew domain registration", "notes": "Expires end of month", "priority": "High",   "done": false },
  { "title": "Reply to design feedback",  "notes": "",                     "priority": "Medium", "done": false },
  { "title": "Book dentist",              "notes": "",                     "priority": "Low",    "done": true  },
  { "title": "Migrate the analytics pipeline to the new warehouse and validate dashboards", "notes": "Long one — check layout", "priority": "Medium", "done": false }
]
```

## 9. UI state model

### Task list

The list is one state object rather than a sealed hierarchy, because a refresh has to spin while
already-loaded rows stay on screen. `TaskListUiState` holds `tasks`, `isLoading` and `error`;
`isEmpty` is derived, so it can never disagree with the list it describes.

| Precedence | Condition | Rendered | Requirement |
|---|---|---|---|
| 1 | `tasks.isEmpty() && isLoading` | `Loading` | FR-07 |
| 2 | `tasks.isEmpty() && error != null` | `ErrorMessage` + Retry | FR-09 |
| 3 | `tasks.isEmpty()`, neither of the above | `EmptyMessage` | FR-08 |
| 4 | otherwise | `LazyColumn` of rows | FR-01 |

A failure arriving while content is already on screen must not blank the list; it is surfaced
transiently (snackbar) instead. That boundary belongs to FR-09 and is scheduled in E5.

### Task editor

| Mode | Trigger | Initial state |
|---|---|---|
| Create | `Route.TaskEditor(taskId = null)` | Empty form, priority `MEDIUM` |
| Edit | `Route.TaskEditor(taskId = "…")` | `Loading`, then the form seeded from `GetTaskUseCase` |

Validation: `canSave` requires a non-blank title and no save or load in flight. A blank title on save
sets `titleError` and does not navigate. `titleError` is deliberately separate from `error`: the
first is a form problem the user fixes in place, the second is a data failure needing a retry.

## 10. Architecture constraints

The backlog must not violate these. They are enforced by the build, not by review.

| Module | May depend on |
|---|---|
| `:lib:*` | nothing — pure Kotlin JVM |
| `:core:common` | `:lib:*` |
| `:core:ui` | `:core:common`, `:lib:tasks-api` |
| `:feature:*` | `:core:*`, `:lib:*` — never another feature, never `:data:*` |
| `:data:*` | `:lib:*`, `:core:common` — never a feature, never `:core:ui` |
| `:app` | everything; the only place a contract is bound to an implementation |

Consequences that shape the backlog:

- Anything two features both need is a `:core` or `:lib` change, never a feature-to-feature import.
- Adding a field to `Task` (FR-13) is a `:lib:tasks-api` change that ripples into `:data:tasks` and
  both features. Its estimate reflects that.
- A feature ViewModel can be tested against `FakeTaskRepository` from `:core:testing` with no data
  module on the classpath.

## 11. Traceability matrix

Every statement in the brief maps to a requirement and an epic. Task IDs are defined in
[BACKLOG.md](BACKLOG.md).

| Brief statement | Requirement | Epic | Tasks |
|---|---|---|---|
| Core 1 — task list screen | FR-01 | E3 | TB-303, TB-304 |
| Core 2 — add a task | FR-02 | E4 | TB-402, TB-403 |
| Core 3 — complete and delete | FR-03, FR-04 | E3 | TB-305, TB-306 |
| Core 4 — detail / edit screen | FR-05 | E4 | TB-401, TB-403 |
| Core 5 — build your own mock network layer | FR-06, NFR-01, NFR-04 | E1, E2 | TB-101…TB-105, TB-201…TB-206 |
| Core 6 — loading, empty, error states | FR-07, FR-08, FR-09 | E3 | TB-302, TB-307 |
| Mock: 300–800 ms delay on reads | NFR-02 | E1 | TB-101 |
| Mock: ~15% failure on load/save | NFR-03 | E1 | TB-101, TB-105 |
| Mock: suspending functions or `Flow` | NFR-01 | E1, E2 | TB-102, TB-203 |
| Sample seed data | §8 | E1 | TB-103 |
| Stretch — search / filter by title | FR-10 | E6 | TB-601, TB-602 |
| Stretch — sort by priority or completion | FR-11 | E6 | TB-603, TB-604 |
| Stretch — undo a delete | FR-12 | E5 | TB-504, TB-505 |
| Stretch — state across config change / process recreation | NFR-06, NFR-07 | E5 | TB-502, TB-503 |
| Stretch — due dates with relative formatting | FR-13 | E7 | TB-701…TB-704 |
| Stretch — theming and dark-mode support | FR-14 | E7 | TB-705, TB-706 |
| Out of scope — real networking | §3 | — | none, by design |
| Out of scope — authentication | §3 | — | none, by design |
| Out of scope — push notifications | §3 | — | none, by design |
| Out of scope — on-disk persistence, multiple modules | §3, D-1, D-2 | E0 | delivered as a deviation, justified in §4 |
| "If you make a deliberate choice … tell us why" | §4 | E8 | TB-801, TB-802 |
| "~10 min to walk us through what you built and what you would do next" | — | E8 | TB-802, TB-803 |
