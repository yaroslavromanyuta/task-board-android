# Task Board — Delivery Backlog

Companion to [REQUIREMENTS.md](REQUIREMENTS.md). Requirement IDs (`FR-…`, `NFR-…`) refer to that
document.

## 1. How to read this

**Structure.** Epic → user story → task. The task is the implementable unit: one task is one
focused change with its own acceptance criterion.

**Estimation.** Fibonacci story points, anchored so estimates stay comparable:

| Points | Means |
|---|---|
| 1 | A small change in one file, no new concept. |
| 2 | One file, some logic or one new test. |
| 3 | Two or three files, or one file with real branching to reason about. |
| 5 | Crosses a module boundary, or introduces a pattern used later. |
| 8 | Touches the domain model and ripples through data and both features. Split it if you can. |

**Definition of Ready.** A task is ready when it names its module and file, its parent story, its
acceptance criterion, and every dependency is done.

**Definition of Done.** All of the following, per task:

1. Code compiles: `./gradlew assembleDebug`.
2. `./gradlew lint` clean (`checkDependencies = true` catches boundary violations).
3. `./gradlew test` green, including the task's own test if it has a sibling test task.
4. No `TODO()` reachable from a user action on the path the task touched (NFR-11).
5. Module dependency rules unviolated (NFR-12).
6. Behaviour observed on a device or emulator, not just in a test.

**Status legend.** ☑ done · ☐ open.

## 2. Epics

| ID | Epic | Requirements | Points | Status |
|---|---|---|---|---|
| E0 | Project foundation and architecture | D-1…D-4, NFR-10, NFR-12 | 21 | ☑ done |
| E1 | Mock network layer | FR-06, NFR-01…NFR-05 | 16 | ☐ |
| E2 | Data layer: cache, repository, use cases | FR-06, NFR-04, NFR-13 | 18 | ☐ |
| E3 | Task list screen | FR-01, FR-03, FR-04, FR-07…FR-09 | 18 | ☐ |
| E4 | Task detail / edit screen | FR-02, FR-05 | 13 | ☐ |
| E5 | Resilience and state handling | FR-12, NFR-06, NFR-07 | 16 | ☐ |
| E6 | Stretch: list shaping | FR-10, FR-11 | 11 | ☐ |
| E7 | Stretch: presentation | FR-13, FR-14 | 16 | ☐ |
| E8 | Handover: documentation and walkthrough | §4, brief closing section | 5 | ☐ |

Total open: 113 points across E1–E8.

## 3. Iteration plan

Each iteration ends in something demoable. That is the point of the split: work can stop at any
iteration boundary and still leave a coherent product.

| Iteration | Goal | Epics | Points | Demo at the end |
|---|---|---|---|---|
| 0 | Walking skeleton | E0 | 21 ☑ | App runs, navigates list → editor → back, renders the empty state |
| 1 | Read path | E1, E2 (read), E3 | 44 | List loads real seed data with a spinner; failures show an error with Retry |
| 2 | Write path | E2 (write), E4 | 21 | Create, edit, complete, delete — full CRUD against the mock source |
| 3 | Resilience | E5 | 16 | Write failures surfaced without blanking the list; undo delete; survives rotation and process death |
| 4 | Stretch | E6, E7 | 27 | Search, sort, due dates, dark mode |
| 5 | Handover | E8 | 5 | README, deviation rationale, "what I would do next" |

**Iteration 1 is deliberately the largest.** It is the vertical slice that turns the skeleton into a
working product; nothing after it is meaningful without it.

## 4. Cut-line

Decided in advance, so that under time pressure nothing has to be re-argued.

| Band | Contents | Rule |
|---|---|---|
| **Must** | Iterations 1 and 2 (E1, E2, E3, E4) | Non-negotiable. These are the brief's six core requirements. |
| **Should** | Iteration 3 (E5), Iteration 5 (E8) | Ship if the Must band is solid. E8 is cheap and disproportionately valuable at the walkthrough. |
| **Could** | Iteration 4 (E6, E7) | Drop first, whole epics at a time. Never start E7 before E6: `TB-701` changes the domain model and half-finishing it leaves the build broken. |

If time runs short mid-iteration, finish the story in hand before starting the next. A half-finished
story is worth less than one fewer story.

---

## 5. Epics, stories and tasks

### E0 — Project foundation and architecture ☑ delivered

Iteration 0, already in the repo. Recorded for traceability; see [../README.md](../README.md).

| ID | Task | Points | Status |
|---|---|---|---|
| TB-001 | Module graph, `settings.gradle.kts`, dependency rules | 5 | ☑ |
| TB-002 | `build-logic` convention plugins, version catalog, minSdk 26 / JVM 17 | 5 | ☑ |
| TB-003 | Domain contracts: `Task`, `TaskPriority`, `TaskDraft`, `DataError`, `TaskRepository`, use case shells | 3 | ☑ |
| TB-004 | `:core:ui` design system: theme, `Loading`/`EmptyMessage`/`ErrorMessage`, `PriorityIndicator`, error wording | 3 | ☑ |
| TB-005 | Hilt graph, type-safe `Route`, `TodoNavHost`, both feature shells | 3 | ☑ |
| TB-006 | `:core:testing`: `MainDispatcherRule`, `TestData`, `FakeTaskRepository` shell | 2 | ☑ |

---

### E1 — Mock network layer

Requirements: FR-06, NFR-01, NFR-02, NFR-03, NFR-05. Specification: REQUIREMENTS.md §8.

> **US-01** — As a developer, I want a fake API that is slow and unreliable in a controlled way, so
> that the loading and error states in the app are real behaviour rather than a simulation added in
> the UI layer.
>
> **Given** the app starts, **when** it loads tasks, **then** the call takes 300–800 ms and fails
> about 15% of the time, and the failure arrives as a typed `DataError`.

| ID | Task | Module / file | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-101 | Implement latency and failure injection: uniform 300–800 ms delay on reads, ~15% failure on read and write, injected `Random` and `Clock`, configurable failure rate. Correct the stale planning comment (it says 200–900 ms and 1-in-7). | `data/tasks/.../api/FakeTaskApi.kt` | 3 | — | Timings and rate match REQUIREMENTS.md §8. Rate is settable to 0. |
| TB-102 | Implement the five `TaskApi` operations over a `MutableList<TaskDto>` guarded by a `Mutex`; source-assigned ids; `createdAt` from the injected `Clock`. | `data/tasks/.../api/FakeTaskApi.kt` | 5 | TB-101 | All five operations round-trip. Concurrent calls do not corrupt the store. |
| TB-103 | Seed the store with the brief's four rows, verbatim, long title included. | `data/tasks/.../api/FakeTaskApi.kt` (or a new `SeedData.kt`) | 1 | TB-102 | Contents match REQUIREMENTS.md §8 exactly. |
| TB-104 | Map thrown failures to `DataError`: dice → `Network`, unknown id → `NotFound` (deterministic, never random). | `data/tasks/.../api/FakeTaskApi.kt` | 2 | TB-102 | Every throw is a `DataException` with a mapped `DataError`. |
| TB-105 | Unit-test `FakeTaskApi` with a seeded `Random` and `TestDispatcher`: latency window, failure rate over N calls, `NotFound` determinism, CRUD round-trip. | `data/tasks/src/test/.../FakeTaskApiTest.kt` (new) | 5 | TB-102, TB-104 | Deterministic — no test depends on the dice (NFR-05). |

---

### E2 — Data layer: cache, repository, use cases

Requirements: FR-06, NFR-01, NFR-04, NFR-13.

> **US-02** — As a developer, I want a repository that is the single source of truth for reads, so
> that every screen renders from one stream and a failed write never leaves the UI showing something
> the source does not have.
>
> **Given** a successful load, **when** a write succeeds, **then** the cache is updated and every
> observer sees the change; **when** a write fails, **then** the cache is untouched.

| ID | Task | Module / file | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-201 | Implement `InMemoryTaskCache.replaceAll` / `upsert` / `remove` over the existing `MutableStateFlow`. | `data/tasks/.../cache/InMemoryTaskCache.kt` | 2 | — | Emissions are observed by `observe()`. `upsert` replaces by id, never duplicates. |
| TB-202 | Implement `TaskDto.toDomain()` and `TaskDraft.toPayload()`. An unknown priority string falls back rather than throwing. | `data/tasks/.../mapper/TaskMapper.kt` | 2 | — | Round-trips. Unknown priority maps to `MEDIUM` and does not crash the list. |
| TB-203 | Implement the read half of `DefaultTaskRepository`: `observeTasks()` from the cache, `refresh()` and `getTask()` through the API on the injected IO dispatcher, wrapped in `suspendRunCatching`. | `data/tasks/.../repository/DefaultTaskRepository.kt` | 3 | TB-101…TB-104, TB-201, TB-202 | Returns `Result`; failures are always `DataException` (NFR-04). |
| TB-204 | Implement the write half: `createTask`, `updateTask`, `setCompleted`, `deleteTask`. API first, cache updated only on success. | `data/tasks/.../repository/DefaultTaskRepository.kt` | 3 | TB-203 | A failed write leaves the cache unchanged. |
| TB-205 | Implement the six use cases. `SaveTaskUseCase` owns title validation (blank title is rejected here, not in the ViewModel) and routes create vs. update on a null id. `ObserveTasksUseCase` owns default ordering. | `lib/tasks-api/.../usecase/*.kt` | 3 | TB-203, TB-204 | Validation lives in exactly one place. |
| TB-206 | Complete `FakeTaskRepository` in `:core:testing` (its `nextError` and `fail()` exist for this) and unit-test `DefaultTaskRepository` and `SaveTaskUseCase`: cache-untouched-on-failure, validation, create-vs-update routing. | `core/testing/.../FakeTaskRepository.kt`, `data/tasks/src/test/.../DefaultTaskRepositoryTest.kt` (new), `lib/tasks-api/src/test/.../SaveTaskUseCaseTest.kt` (new) | 5 | TB-205 | `./gradlew test` green. `FakeTaskRepository` has no `TODO()` left. |

---

### E3 — Task list screen

Requirements: FR-01, FR-03, FR-04, FR-07, FR-08, FR-09. State model: REQUIREMENTS.md §9.

> **US-03** — As a task owner, I want to see all my tasks in one scrollable list with their priority
> visible, so that I can tell at a glance what needs attention.
>
> **US-04** — As a task owner, I want the app to tell me when it is loading, when I have nothing,
> and when something went wrong, so that I am never looking at a blank screen wondering.
>
> **Given** the app is loading with nothing cached, **when** I open the list, **then** I see a
> spinner; **when** the load fails, **then** I see a typed message and a Retry control; **when** it
> succeeds with no tasks, **then** I see an empty message.
>
> **US-05** — As a task owner, I want to complete or delete a task straight from the list, so that I
> do not have to open it first.

| ID | Task | Module / file | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-301 | Collect `ObserveTasksUseCase` into `TaskListUiState` in `viewModelScope`; trigger the first `refresh()` on init. | `feature/task-list/.../TaskListViewModel.kt` | 3 | TB-205 | The list renders seed data on launch. |
| TB-302 | Implement `onRetry`: set `isLoading`, call `RefreshTasksUseCase`, map failure to `state.error`, clear it on success. | `feature/task-list/.../TaskListViewModel.kt` | 2 | TB-301 | The four-state precedence table in REQUIREMENTS.md §9 holds. |
| TB-303 | Verify `TaskListScreen` against the real states, including the long-title row; fix any layout or truncation defect the seed data exposes. | `feature/task-list/.../TaskListScreen.kt`, `component/TaskRow.kt` | 2 | TB-301 | The long title truncates cleanly; the row does not grow unbounded (FR-01). |
| TB-304 | Confirm keyed rows and stable recomposition on the populated list. | `feature/task-list/.../TaskListScreen.kt` | 1 | TB-303 | `key = { it.id }` present; toggling one row does not recompose the whole list (NFR-08). |
| TB-305 | Implement `onToggleCompleted` through `ToggleTaskCompletedUseCase`. | `feature/task-list/.../TaskListViewModel.kt` | 2 | TB-205 | The row updates and the change survives a refresh (FR-03). |
| TB-306 | Implement `onDelete` through `DeleteTaskUseCase`. | `feature/task-list/.../TaskListViewModel.kt` | 2 | TB-205 | The task disappears and stays gone after a refresh (FR-04). |
| TB-307 | Unit-test `TaskListViewModel` with `FakeTaskRepository` + `MainDispatcherRule` + Turbine: loading → content, loading → error → retry → content, empty, toggle, delete. | `feature/task-list/src/test/.../TaskListViewModelTest.kt` (new) | 5 | TB-302, TB-305, TB-306, TB-206 | Every branch of the state table has a test. |
| TB-308 | Verify accessibility on the list: priority label read out, delete and checkbox described. | `feature/task-list/…`, `core/ui/.../PriorityIndicator.kt` | 1 | TB-303 | NFR-09 satisfied. |

---

### E4 — Task detail / edit screen

Requirements: FR-02, FR-05.

> **US-06** — As a task owner, I want to add a task with a title, optional notes and a priority, so
> that I can capture something quickly.
>
> **US-07** — As a task owner, I want to tap a task and edit its fields, so that I can correct or
> refine it later.
>
> **Given** I open an existing task, **when** the screen loads, **then** the form is seeded with its
> current values; **when** I save, **then** the change is reflected in the list.

| ID | Task | Module / file | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-401 | Implement the edit-mode load: when `route.taskId != null`, call `GetTaskUseCase`, show `Loading`, seed the form, map failure to `state.error`. Implement `onRetry` to re-run it. | `feature/task-editor/.../TaskEditorViewModel.kt` | 3 | TB-205 | Editing an existing task shows its current values (FR-05). |
| TB-402 | Implement `onSave` through `SaveTaskUseCase`: set `isSaving`, report success only on `Result.success`, surface failure as `state.error` without navigating away. | `feature/task-editor/.../TaskEditorViewModel.kt` | 3 | TB-205 | A failed save does not lose the user's input (FR-02). |
| TB-403 | Confirm create and edit both round-trip end to end through navigation; the list reflects the change on return. | `app/.../navigation/TodoNavHost.kt`, both feature modules | 2 | TB-401, TB-402, TB-301 | Add and edit are both visible in the list without a manual refresh. |
| TB-404 | Unit-test `TaskEditorViewModel`: create mode, edit mode seeding, blank-title validation, save failure keeps the form, `SavedStateHandle` argument reading. | `feature/task-editor/src/test/.../TaskEditorViewModelTest.kt` (new) | 5 | TB-401, TB-402, TB-206 | Both modes covered from one test class. |

---

### E5 — Resilience and state handling

Requirements: FR-12, NFR-06, NFR-07, and the FR-09 boundary case.

> **US-08** — As a task owner, I want a failure while I already have tasks on screen to be reported
> without wiping the list, so that a flaky moment does not cost me my context.
>
> **US-09** — As a task owner, I want a deleted task to be recoverable, so that a mis-tap is not
> permanent.
>
> **US-10** — As a task owner, I want to rotate my phone or come back to the app without losing what
> I typed.

| ID | Task | Module / file | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-501 | Surface write and refresh failures transiently via a `SnackbarHost` when content is already on screen, instead of replacing the list with an error state. | `feature/task-list/.../TaskListScreen.kt`, `TaskListViewModel.kt` | 3 | TB-302 | A failed toggle shows a snackbar; the list stays populated (FR-09 boundary). |
| TB-502 | Back the editor form fields with `SavedStateHandle` so a half-typed form survives process recreation. | `feature/task-editor/.../TaskEditorViewModel.kt` | 3 | TB-402 | "Don't keep activities" on: the typed title survives (NFR-07). |
| TB-503 | Verify rotation on both screens; fix anything lost. | both feature modules | 2 | TB-502 | Scroll position and form contents survive rotation (NFR-06). |
| TB-504 | Implement undo-delete: retain the deleted task, offer Undo in the snackbar, re-create it on action. | `feature/task-list/.../TaskListViewModel.kt`, `TaskListScreen.kt` | 5 | TB-306, TB-501 | Undo restores the task with its fields intact (FR-12). |
| TB-505 | Unit-test undo and transient error surfacing. | `feature/task-list/src/test/.../TaskListViewModelTest.kt` | 3 | TB-504 | Undo path and snackbar-event path both covered. |

---

### E6 — Stretch: list shaping

Requirements: FR-10, FR-11.

> **US-11** — As a task owner with many tasks, I want to search by title and reorder the list, so
> that I can find and prioritise without scrolling.

| ID | Task | Module / file | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-601 | Add `query` to `TaskListUiState` and derive the filtered list. Filtering happens over the cached list, not by re-querying the source. | `feature/task-list/.../TaskListUiState.kt`, `TaskListViewModel.kt` | 3 | TB-301 | Typing narrows the list; clearing restores it (FR-10). |
| TB-602 | Add the search field to the app bar and a distinct "no matches" message, separate from "no tasks at all". | `feature/task-list/.../TaskListScreen.kt`, `core/ui` strings | 2 | TB-601 | The two empty states read differently. |
| TB-603 | Add a sort mode (priority, completion, default) to the state and apply it in `ObserveTasksUseCase` or the ViewModel's derivation. | `lib/tasks-api/.../usecase/ObserveTasksUseCase.kt`, `feature/task-list/…` | 3 | TB-301 | Order changes and holds while the screen is open (FR-11). |
| TB-604 | Sort control in the UI, plus tests for filter and sort derivation. | `feature/task-list/.../TaskListScreen.kt`, `…/TaskListViewModelTest.kt` | 3 | TB-602, TB-603 | Derivations tested without touching the source. |

---

### E7 — Stretch: presentation

Requirements: FR-13, FR-14.

> **US-12** — As a task owner, I want due dates shown in human terms, so that I can judge urgency
> without doing date arithmetic.
>
> **US-13** — As a task owner, I want the app to respect my system theme.

> **Change impact.** TB-701 adds a field to the domain model. It ripples through `:lib:tasks-api`,
> `:data:tasks` (DTO, mapper, seed) and both feature modules. Do not start it unless E6 is finished
> and there is room to finish it — a half-applied model change leaves the build broken.

| ID | Task | Module / file | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-701 | Add `dueDate: Instant?` to `Task` and `TaskDraft`; extend `TaskDto`/`TaskPayload` and the mapper; extend the seed rows. | `lib/tasks-api/.../model/`, `data/tasks/.../api/model/`, `mapper/`, seed | 5 | E6 complete | The whole graph compiles; existing tests still pass. |
| TB-702 | Relative date formatting helper. | `core/ui/.../format/RelativeDate.kt` (new) | 3 | TB-701 | "tomorrow", "in 3 days", "2 days ago"; unit-tested against a fixed `Clock`. |
| TB-703 | Show the due date on the list row. | `feature/task-list/.../component/TaskRow.kt` | 2 | TB-702 | Renders; absent date renders nothing, not "null" (FR-13). |
| TB-704 | Date picker in the editor. | `feature/task-editor/.../TaskEditorScreen.kt` | 3 | TB-701 | Set and clear both work. |
| TB-705 | Audit dark mode: verify the theme in both schemes and check the priority colours hold their contrast. | `core/ui/.../theme/` | 2 | — | Legible in both; priority colours stay distinguishable (FR-14). |
| TB-706 | Add dark-mode previews for the shared components and both screens. | `core/ui/…`, both feature modules | 1 | TB-705 | Each screen has a light and a dark preview. |

---

### E8 — Handover: documentation and walkthrough

The brief allocates ~10 minutes to explaining what was built and what would come next, and asks for
the reasoning behind any deliberate deviation. That is deliverable work, not an afterthought.

| ID | Task | File | Pts | Depends on | Acceptance |
|---|---|---|---|---|---|
| TB-801 | Update `README.md` to reflect the delivered state: which iterations landed, what is stubbed, and the deviation rationale from REQUIREMENTS.md §4. | `README.md` | 2 | last shipped iteration | Someone cloning the repo can tell what works without running it. |
| TB-802 | Write walkthrough notes: the module-graph story, why multi-module, why in-memory, how the mock source produces the three states, and what was consciously cut. | `docs/WALKTHROUGH.md` (new) | 2 | TB-801 | Covers every deviation in §4 and every item left below the cut-line. |
| TB-803 | Write the "what I would do next" list: Room behind `InMemoryTaskCache`, real `TaskApi` implementation, paging, screenshot tests, CI. | `docs/WALKTHROUGH.md` | 1 | TB-802 | Each item names the seam it would land on. |

---

## 6. Risk register

| ID | Risk | L / I | Mitigation |
|---|---|---|---|
| R-1 | The ~15% failure rate makes a live demo unpredictable — an error appears at the wrong moment, or never appears when it should be shown. | High / Med | TB-101 makes the rate configurable and the `Random` injectable. Set it to 0 to demo the happy path and to 1.0 to demo the error state on cue. |
| R-2 | The multi-module structure reads as over-engineering against a brief that says a single module is fine. | Med / Med | REQUIREMENTS.md §4 D-1 gives the rationale; TB-802 makes it a spoken talking point rather than something the reviewer has to infer. |
| R-3 | Stretch goals crowd out core polish, producing the "rushed pile of half-features" the brief warns against. | Med / High | The cut-line in §4 is decided in advance. E6 and E7 are dropped whole, never partially. |
| R-4 | Process-death restoration (NFR-07) is hard to demonstrate convincingly. | Med / Low | Verify with `adb shell am kill com.rounds.test.to_dolist` or Developer Options → "Don't keep activities"; note the method in TB-802. |
| R-5 | TB-701 (due dates) is started and not finished, leaving the domain model half-migrated. | Low / High | Gated behind "E6 complete" and flagged in the E7 change-impact note. |
| R-6 | Test tasks are the first thing cut under pressure, despite NFR-13. | Med / Med | Test tasks are siblings of their feature task inside the same story, not a trailing batch, so cutting one visibly breaks its story. |

## 7. Open questions

| ID | Question | Owner | Impact if unresolved |
|---|---|---|---|
| Q-1 | Should completed tasks sort to the bottom by default, or hold their position? The brief specifies neither. | Product | Affects TB-205 default ordering and TB-603. Assumption until answered: hold position; sorting stays explicit (FR-11). |
| Q-2 | On a failed delete, should the row reappear immediately or after the next refresh? | Product | Affects TB-204 and TB-306. Assumption: the cache is only updated on success, so the row never disappears in the first place. |
| Q-3 | Is `Timeout` worth simulating separately from `Network`, or is one failure mode enough? | Engineering | Affects TB-104. Assumption: `Network` only, with `Timeout` reserved in the taxonomy for later. |
