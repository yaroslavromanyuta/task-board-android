# Task Board — walkthrough

Notes for the ~10 minutes the brief allocates to explaining what was built, why, and what would come
next. Companion to [REQUIREMENTS.md](REQUIREMENTS.md) and [BACKLOG.md](BACKLOG.md).

## 1. What shipped

Every core requirement and four of the six stretch goals, delivered as five iterations that each
ended in something demoable.

| Iteration | Epics | What it made true |
|---|---|---|
| 0 | E0 | The module graph, DI, navigation and every contract compile; the app runs and navigates |
| 1 | E1–E3 | The mock source, the data layer, the list — real loading, empty and error states |
| 2 | E4 | The editor loads, seeds and saves; full CRUD |
| 3 | E5, E6 | Failures reported without blanking the list, undo-delete, form survives process death, search and sort |
| 4 | E7 | Due dates in human terms, dark-mode palette |
| 5 | E8 | This document |

Left below the cut-line: nothing. The backlog is closed.

Not built, deliberately: real networking, authentication, push notifications and on-disk persistence,
all four excluded by the brief.

**Numbers.** Nine modules, 77 unit tests, no `TODO()` anywhere in the source, `./gradlew lint` clean.

## 2. The module graph, and why there is one

The brief says a single module is fine and that multiple modules are not needed. There are nine. That
is the largest deviation in the project and the one worth the most of the ten minutes.

```
                        :app
                          │  composition root: NavHost + the only module that sees :data
        ┌─────────────────┼──────────────────┬──────────────────┐
        │                 │                  │                  │
:feature:task-list  :feature:task-editor  :data:tasks   (binds contracts → impls)
        │                 │                  │
        └────────┬────────┘                  │
                 ▼                           ▼
          :core:ui  :core:common ──────► :lib:tasks-api
                 └──────────────────────► :lib:navigation-api
```

The argument is not "big projects have modules". It is that **the boundaries stop being conventions
and start being compile errors**:

- `:lib:tasks-api` is pure Kotlin JVM, so `import android.os.Parcelable` in the domain does not
  compile. There is no code review step that can be skipped here.
- A feature module cannot reference `DefaultTaskRepository`, because `:data:tasks` is not on its
  compile classpath. It resolves `TaskRepository` and never learns an implementation exists.
- A feature cannot import another feature. Both navigate by a type from `:lib:navigation-api`, and
  `:app` is the only place a route meets the composable behind it.

Both violations were introduced on purpose during iteration 0 and confirmed to fail the build.

The cost is real and worth naming: nine `build.gradle.kts` files and a `build-logic/` directory of
convention plugins so they do not repeat the same `android {}` block nine times. A feature's build file
is now a plugin id and a namespace, and the SDK levels live in one object.

**The other three deviations** (REQUIREMENTS §4): in-memory rather than on disk, which the brief allows
and which is discussed below; `minSdk 26`, the floor the brief permits, chosen so `java.time` works
without desugaring; and the convention plugins, which exist only because of the first deviation.

## 3. How the mock source produces the three states

This is the part the brief specifies most tightly, so it is worth showing rather than describing.

`FakeTaskApi` is shaped like a backend, not like a local list. Five REST-shaped operations, a
`MutableList<TaskDto>` behind a `Mutex`, and every call:

1. draws its latency and its dice **under the lock**, so a seeded `Random` produces the same sequence
   however calls interleave;
2. delays 300–800 ms **outside** the lock, so concurrent callers overlap the way they would against a
   real server;
3. throws `DataException(DataError.Network)` if the dice said so — about 15% of the time.

An unknown id is `DataError.NotFound`, deterministically, never a dice roll.

So loading is a state because the source is genuinely slow, and the error state appears because the
source genuinely fails — neither is simulated in a ViewModel. `failureRate` is a `var`: **set it to
`0.0` to demo the happy path and `1.0` to demo the error state on cue.** That switch exists because a
15% failure rate makes a live demo unpredictable (risk R-1), and because `Random` is injected, no test
depends on a roll it did not choose.

Above it, one rule decides what a failure looks like: **content on screen means the failure passes
over it; nothing on screen means it becomes the screen.** That is REQUIREMENTS §9's boundary case
living in one place — `TaskListViewModel.reportFailure` — instead of being re-decided at every call
site. A flaky moment costs the user a snackbar, not their list.

## 4. Why in-memory, and where the seam is

The brief allows it, so this is agreement rather than deviation. It is recorded because the cache is a
*named* seam, not an assumption spread through the code.

`InMemoryTaskCache` is the single source of truth for reads; `TaskApi` is the source of truth for
writes. Every write goes to the source first and updates the cache **only on success**, so a failed
request can never leave the list showing something the server does not have. Replacing the cache with
Room touches that one file and `DataModule`, and nothing above them.

The same reasoning explains `TaskApi` being an interface with a fake implementation rather than a fake
baked into the repository: `DefaultTaskRepository` is already written against a contract a Retrofit
service could satisfy unchanged.

## 5. Four decisions that were not obvious

**`TaskApi` has no "set completed" operation.** Five REST-shaped calls are the contract, so the flag
travels in the update payload and `TaskRepository.setCompleted` turns one into the other — reading the
task's current fields first, so a stale row cannot overwrite a title edited elsewhere. Bending the
transport to suit one screen would have made the mock stop looking like the thing it exists to imitate.

**Undo re-creates; it does not resurrect.** For the same reason, there is no restore operation, so
`RestoreTaskUseCase` creates the task again and re-applies `isCompleted` in a second call — the flag
cannot travel in a `TaskDraft`, which is deliberately the editable half of a task. The restored task
therefore gets a new id and lands where a new one would. Every field the user can see survives.

**The editor reads its argument by name, not through `toRoute()`.** `savedStateHandle.toRoute()`
decodes through an Android runtime; in a JVM unit test it does not throw, it silently returns
`taskId = null`. Every edit-mode test would have exercised create mode and passed for the wrong reason.
Rather than add Robolectric for one class, `Route.TaskEditor` publishes `TASK_ID_ARG` and `RouteTest`
asserts that constant still matches the serialised property — so renaming the field is a build failure,
not a runtime break with no test to catch it.

**Validation lives in `SaveTaskUseCase`, not in the ViewModel.** The editor's Save button is disabled
on a blank title, but that is an affordance; the rule is the use case returning
`ValidationException.BlankTitle`, which the ViewModel maps onto the field the user has to fix. One rule,
one place, and a second caller cannot drift from it.

## 6. What was consciously cut

Nothing in the backlog. What is missing is a level below it, and each item has a reason rather than an
omission:

- **Instrumented and screenshot tests.** All 77 tests are JVM unit tests: no Robolectric, no device.
  That was a deliberate trade — see §7 for where they would land.
- **`Timeout` and `Conflict` are unemitted.** They exist in `DataError` because the wording differs,
  but the mock only produces `Network` and `NotFound`. A category nothing can emit is a category
  nobody tests (open question Q-3).
- **Search and sort are not persisted across process death.** NFR-07 asks for the editor's form, and a
  search box that empties itself after the app was killed is not a defect worth the code.
- **Paging.** The list holds every task in memory and filters over the cached list. At four rows, and
  at four hundred, that is right; the seam for changing it is §7.

## 7. What I would do next

Each item names the seam it lands on, because that is the point of having named them.

| # | Change | Where it lands |
|---|---|---|
| 1 | **Room behind the cache.** Persist across process death and make the app useful offline. | `InMemoryTaskCache` and `DataModule`. Nothing above the repository changes — `observeTasks()` is already a `Flow` off the cache. |
| 2 | **A real `TaskApi`.** Retrofit or Ktor, swapped in as a one-line `@Binds` change. | `DataModule.bindTaskApi`. `DefaultTaskRepository` is already written against the interface, so it does not move. |
| 3 | **A sync strategy.** Once 1 and 2 are both real, "cache is truth for reads, API for writes" needs a conflict policy — `DataError.Conflict` exists and is currently unemitted, which is where it starts earning its place. | `DefaultTaskRepository`, plus a `WorkManager` job for retries. |
| 4 | **Paging.** `observeTasks()` becomes `Flow<PagingData<Task>>` when the list stops fitting in memory; the search filter moves from `TaskListUiState` back down to the query. | `TaskRepository`, `ObserveTasksUseCase`, `TaskListViewModel`. |
| 5 | **Screenshot tests.** Every screen already has a light and a dark `@PreviewLightDark`, which is most of the work; Paparazzi or Roborazzi would turn those into assertions. | The existing previews in both feature modules and `:core:ui`. |
| 6 | **Compose UI tests.** The stateless `…Screen` composables take a state and callbacks and nothing else, so they test without Hilt. The state table in REQUIREMENTS §9 is the test plan. | `src/androidTest` in both feature modules. |
| 7 | **CI.** `assembleDebug`, `test` and `lint` on every PR — the same three commands each iteration was verified with, run by a machine instead of by hand. | A GitHub Actions workflow; the convention plugins already make the commands uniform across modules. |
| 8 | **Accessibility beyond inspection.** Priority is conveyed by text as well as colour and every icon button carries a description, but that was verified by reading the code, not with TalkBack on a device. | Both feature modules; `:core:ui`'s components. |

## 8. If there were more time in the exercise

The honest answer to "what would you change about the approach": the multi-module structure is a bet
that this is the start of a real product, exactly as the brief frames it. If the framing were "ship a
demo tomorrow", one module would be the right call and none of the layering above would earn its keep.
That bet is the thing worth disagreeing with, and it is stated in REQUIREMENTS §4 D-1 so it can be
argued with rather than discovered.
