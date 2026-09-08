# To-do list

Multi-module Clean Architecture skeleton for a task list app: Kotlin, Jetpack Compose, Hilt,
Navigation Compose with type-safe routes.

**Complete: every core requirement and every stretch goal, across five iterations.** The app launches
into the task list, loads the brief's seed data through a mock network source that is slow (300-800 ms)
and fails about 15% of the time, and renders loading, empty and error+retry as real consequences of
that source rather than as simulations. Tasks can be created, edited, completed and deleted, every one
of them through that source. A failure that arrives with rows on screen is reported over the list
instead of replacing it, a deleted task can be undone, the list can be searched and sorted, due dates
read as "tomorrow" and "2 days ago", the palette holds up in both themes, and a half-typed form
survives the process being killed.

77 unit tests, no `TODO()` anywhere in the source, `./gradlew lint` clean.

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
| `:core:ui` | theme, `Loading` / `EmptyMessage` / `ErrorMessage`, `PriorityIndicator`, error wording, relative dates |
| `:core:testing` | `MainDispatcherRule`, `FakeTaskRepository`, `TestData` - pure Kotlin JVM, so `:lib` tests can use it too |
| `:data:tasks` | `TaskApi` + `FakeTaskApi`, `SeedData`, DTOs and mappers, `InMemoryTaskCache`, `DefaultTaskRepository`, DI |
| `:feature:task-list` | list screen: UiState, ViewModel, Route/Screen split, `TaskRow`, nav section |
| `:feature:task-editor` | create/view/edit form: UiState, ViewModel, Route/Screen split, nav section |
| `:app` | `TodoListApplication`, `MainActivity`, `TodoNavHost` |

## Documentation

- [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) — technical requirements, mock network spec, UI state
  model, traceability against the source brief.
- [docs/BACKLOG.md](docs/BACKLOG.md) — epics, user stories, tasks, iteration plan, cut-line, risks.
- [docs/WALKTHROUGH.md](docs/WALKTHROUGH.md) — **start here**: what shipped, why the module graph
  exists, how the mock source produces the three states, what was consciously cut, and what would come
  next.

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
`SavedStateHandle`, which keeps the nav payload small and survives process death. The form's own
fields are written through to the same handle on every change, so a half-typed task survives the
process being killed in the background; a restored form is never reloaded over, because the source's
values are older than what the user typed. It reads that id by
the name `Route.TaskEditor` publishes rather than through `toRoute()`: `toRoute()` decodes through an
Android runtime and quietly returns nothing in a JVM unit test, which would have left the whole edit
path untestable without Robolectric. `RouteTest` asserts the name still matches the serialised
property, so the two cannot drift.

**A failure is reported over the screen or instead of it, never both.** Each screen splits its failure
state in two. On the list, `error` is a load that failed with nothing cached, and takes the screen with
a Retry; `message` is anything that went wrong - or a delete that succeeded - while rows were already
showing, and passes over them in a snackbar. The ViewModel picks by one rule, "is there content on
screen", so REQUIREMENTS.md section 9's boundary case lives in one place rather than being re-decided
per call site.

**Undo re-creates the task rather than resurrecting it.** `TaskApi` has the five REST-shaped
operations a backend would offer and none of them restores a deleted row, so `RestoreTaskUseCase`
creates the task again and re-applies `isCompleted` - the flag cannot travel in a `TaskDraft`, which is
deliberately the editable half of a task. The restored task gets a new source-assigned id and appears
where a new one would; every field the user can see survives. Bending the transport to preserve the id
would have made the mock stop looking like a backend, which is the one thing it exists to look like.

**Search and sort are derivations, not queries.** `TaskListUiState` holds `tasks` (everything cached)
and computes `visibleTasks` from the query and the sort mode. Keeping both is what lets "no tasks yet"
and "nothing matches" be different sentences. `TaskSort` lives in `:lib:tasks-api` rather than in the
screen, for the same reason `TaskPriority` is declared low-to-high: the ordering is a fact about tasks,
and a second surface must not be free to invent a different one.

**The editor distinguishes a failed load from a failed save.** A load failure means there is nothing
to edit, so the form gives way to a message and a Retry. A save failure means the form is still good
and still full of the user's work, so it arrives as a snackbar and nothing is lost (FR-02). Success is
reported as state (`isSaved`) and acted on by the route, not through a callback handed to the
ViewModel - the save is asynchronous, and navigation belongs where the composable is.

**The priority palette has a light set and a dark one, and the theme says which.** Those three colours
sit outside the Material scheme deliberately, so that dynamic colour cannot make "high" look calmer
than "low" - which means nothing in the scheme can adapt them either. `TodoListTheme` publishes
`LocalIsDarkTheme` so the palette is chosen from the theme rather than from the system, and a preview
forced into one scheme gets the matching set.

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
The FAB opens the editor for a new task and tapping a row opens it seeded with that task's values;
Save writes through the same source and the list reflects the change on return, with no manual
refresh. A save that loses the dice roll reports itself without clearing the form.

To see a specific state on demand, set `FakeTaskApi.failureRate` to `0.0` or `1.0`.

## What is tested

`./gradlew test` runs 77 cases: the mock source (latency window, failure rate, `NotFound`
determinism, CRUD round-trip, concurrent writes against a real dispatcher), the repository (cache
untouched on a failed write, every failure typed as `DataException`, completion preserved across an
edit), `SaveTaskUseCase` (validation, create-vs-update routing), `TaskListViewModel` (every branch of
the state table, the transient-failure boundary, undo including a completed task, and the search and
sort derivations) and `TaskEditorViewModel` (both modes, seeding, failed load with retry, failed save
keeping the form, and a recreated process restoring the form without reloading over it). `TaskSort` and
`RestoreTaskUseCase` are tested in the domain module, where they live, and `RelativeDate`'s arithmetic
is pinned against a fixed "now" and a fixed time zone.

## Where to start reading

[docs/WALKTHROUGH.md](docs/WALKTHROUGH.md) — the module-graph argument, the mock source, the decisions
that were not obvious, and what was consciously cut.

## Next step

The backlog is closed, so "next" is no longer an iteration - it is the list in
[docs/WALKTHROUGH.md](docs/WALKTHROUGH.md) §7, each item named against the seam it would land on: Room
behind `InMemoryTaskCache`, a real `TaskApi` bound in `DataModule`, a sync strategy once both are real,
paging, screenshot tests over the existing `@PreviewLightDark` previews, Compose UI tests against the
stateless screens, CI running the same three commands each iteration was verified with, and
accessibility verified with TalkBack rather than by inspection.
