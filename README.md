# To-do list

Multi-module Clean Architecture skeleton for a task list app: Kotlin, Jetpack Compose, Hilt,
Navigation Compose with type-safe routes.

This commit is **structure, not behaviour**. The module graph, DI, navigation and the contracts
between layers are complete and the app runs; the mock network layer and the logic on top of it are
the next step and are marked `TODO` at the exact seams where they belong.

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
| `:core:testing` | `MainDispatcherRule`, `FakeTaskRepository`, `TestData` |
| `:data:tasks` | `TaskApi` + `FakeTaskApi`, DTOs and mappers, `InMemoryTaskCache`, `DefaultTaskRepository`, DI |
| `:feature:task-list` | list screen: UiState, ViewModel, Route/Screen split, `TaskRow`, nav section |
| `:feature:task-editor` | create/view/edit form: UiState, ViewModel, Route/Screen split, nav section |
| `:app` | `TodoListApplication`, `MainActivity`, `TodoNavHost` |

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
repository is written against a contract that a Retrofit service could satisfy unchanged.

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

The app launches to the task list showing its empty state, and the FAB opens the editor, where the
form is live and Save stays disabled until the title is non-blank. Nothing reaches the data layer
yet.

## Next step

Implement the mock network layer in `FakeTaskApi`: an in-memory store behind a `Mutex`,
`delay(200..900 ms)` per call, and roughly a 1-in-7 chance of `DataError.Network` or `Timeout`, so the
loading and error states the UI already handles are produced by the source rather than simulated in a
ViewModel. Then fill in the mappers, the cache writes, `DefaultTaskRepository`, the use cases and the
two ViewModels — the graph does not change.
