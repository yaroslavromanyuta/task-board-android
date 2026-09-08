# To-do list

Multi-module Clean Architecture skeleton for a task list app: Kotlin, Jetpack Compose, Hilt,
Navigation Compose with type-safe routes.

**Delivered: iterations 0 and 1.** The app launches into the task list, loads the brief's seed data
through a mock network source that is slow (300-800 ms) and fails about 15% of the time, and renders
loading, empty and error+retry as real consequences of that source rather than as simulations. Tasks
can be completed and deleted straight from the list. The detail/edit screen is navigable but still
stubbed - that is iteration 2 (E4).

## Module graph

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

:core:testing — test doubles, consumed as testImplementation
```

### Dependency rules

| Module | May depend on |
|---|---|
| `:lib:*` | nothing — pure Kotlin JVM |
| `:core:common` | `:lib:*` |
| `:core:ui` | `:core:common`, `:lib:tasks-api` |
| `:feature:*` | `:core:*`, `:lib:*` — never another feature, never `:data:*` |
| `:data:*` | `:lib:*`, `:core:common` — never a feature, never `:core:ui` |
| `:app` | everything; the only place a contract is bound to an implementation |

These are not conventions to remember — they are enforced by the build. `import android.os.Parcelable`
in `:lib:tasks-api` does not compile (no Android on the classpath), and a feature module cannot
reference `DefaultTaskRepository` because `:data:tasks` is not on its compile classpath. Both were
verified by temporarily introducing the violation.

### What each module holds

| Module | Contents |
|---|---|
| `:lib:tasks-api` | `Task`, `TaskPriority`, `TaskDraft`, `DataError`, `TaskRepository`, use cases |
| `:lib:navigation-api` | `Route` — the type-safe destination contract shared by both features |
| `:core:common` | dispatcher qualifiers + Hilt module, `Clock`, `suspendRunCatching` |
| `:core:ui` | theme, `Loading` / `EmptyMessage` / `ErrorMessage`, `PriorityIndicator`, error wording |
| `:core:testing` | `MainDispatcherRule`, `FakeTaskRepository`, `TestData` - pure Kotlin JVM, so `:lib` tests can use it too |
| `:data:tasks` | `TaskApi` + `FakeTaskApi`, `SeedData`, DTOs and mappers, `InMemoryTaskCache`, `DefaultTaskRepository`, DI |
| `:feature:task-list` | list screen: UiState, ViewModel, Route/Screen split, `TaskRow`, nav section |
| `:feature:task-editor` | create/view/edit form: UiState, ViewModel, Route/Screen split, nav section |
| `:app` | `TodoListApplication`, `MainActivity`, `TodoNavHost` |

## Documentation

- [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) — technical requirements, mock network spec, UI state
  model, traceability against the source brief.
- [docs/BACKLOG.md](docs/BACKLOG.md) — epics, user stories, tasks, iteration plan, cut-line, risks.

## Design decisions

**One `lib` module per shared contract, not a shared "common" dumping ground.** Features need to know
about tasks and about each other's destinations. Both are expressed as pure-Kotlin contracts, so a
feature depends on a *type*, never on another feature's module.

**`:feature:task-editor` serves both "add" and "view / edit".** They are the same form over the same
fields; the only difference is whether navigation supplied a `taskId`. Splitting them into two modules
would duplicate the form to make a diagram look tidier.

**In-memory cache, no database.** The repository is the single source of truth: the cache backs
`observeTasks()`, and the mock API backs every write. That is enough to make loading / empty / error
real states and to keep the list rendering while a refresh is in flight. Durability across process
death is not in the brief. Swapping the cache for Room later touches `InMemoryTaskCache` and
`DataModule` and nothing above them.

**`TaskApi` is an interface with a fake implementation, not a fake baked into the repository.** The
repository is written against a contract that a Retrofit service could satisfy unchanged. It has five
REST-shaped operations and no "set completed": the flag travels in `TaskPayload`, and
`TaskRepository.setCompleted` is what turns one into the other, so the shape a real backend would
offer is not bent to suit one screen.

**The mock source's dice are injected and its failure rate is a `var`.** `FakeTaskApi` takes a
`Random` and a `Clock`, so tests seed both and nothing depends on a roll the test did not choose
(NFR-05). `failureRate` defaults to the brief's 0.15 and can be set to `0.0` to demo the happy path or
`1.0` to demo the error state on cue.

**Screens are split into `…Route` (stateful) and `…Screen` (stateless).** The screen previews and
tests without Hilt; the route is the only place that knows a ViewModel exists.

**Route arguments carry an id, not an object.** The editor refetches from the id it receives through
`SavedStateHandle`, which keeps the nav payload small and survives process death.

**Errors are typed (`DataError`), and wording lives in `:core:ui`.** The domain module needs no
resources and no locale; the UI decides how a failure reads.

## Build setup

Convention plugins in `build-logic/` (`todo.android.library`, `todo.android.library.compose`,
`todo.android.hilt`, `todo.android.feature`, `todo.jvm.library`) hold the shared Android
configuration, so a feature module's build file is a plugin id and a namespace. SDK levels live in
one place, `AndroidSdk` in `build-logic`.

- minSdk 26, targetSdk / compileSdk 37, JVM target 17
- AGP 9.4.0, Gradle 9.6, Kotlin 2.3.20, KSP 2.3.11, Hilt 2.60.1, Compose BOM 2026.03.01

## Running it

```bash
./gradlew :app:assembleDebug     # whole graph compiles
./gradlew test                   # unit tests
./gradlew lint                   # checkDependencies = true, so it follows module dependencies
./gradlew projects               # module graph
./gradlew installDebug           # onto a connected device or emulator
```

The app launches into the list, shows a spinner for as long as the source takes, and then renders the
four seed rows. Roughly one launch in seven fails instead, showing the typed error and a Retry that
re-issues the load. The checkbox and the delete button on a row both round-trip through the source.
The FAB opens the editor, where the form is live and Save stays disabled until the title is non-blank,
but saving does not persist yet.

To see a specific state on demand, set `FakeTaskApi.failureRate` to `0.0` or `1.0`.

## What is tested

`./gradlew test` covers the mock source (latency window, failure rate, `NotFound` determinism, CRUD
round-trip, concurrent writes against a real dispatcher), the repository (cache untouched on a failed
write, every failure typed as `DataException`, completion preserved across an edit), `SaveTaskUseCase`
(validation, create-vs-update routing) and `TaskListViewModel` (every branch of the state table).

## Next step

Iteration 2 in [docs/BACKLOG.md](docs/BACKLOG.md) — E4, the detail/edit screen: seed the form from
`GetTaskUseCase` when navigation supplied a `taskId`, save through `SaveTaskUseCase`, and surface a
failed save without losing what the user typed. The data layer underneath it is already in place, so
the change is confined to `TaskEditorViewModel` and its test. The graph does not change.
