# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a **skeleton, not a finished app**. The module graph, DI, navigation and the contracts between
layers are complete and the app runs; the mock network layer, repository, use cases and ViewModel logic
are still `TODO(...)` at the exact seams where they belong (`FakeTaskApi`, `DefaultTaskRepository`, the
`:lib:tasks-api` use cases, both ViewModels, `FakeTaskRepository`).

Before implementing anything, read the two spec documents — they are the authority, not this file:

- `docs/REQUIREMENTS.md` — FR/NFR ids, the mock network spec (§8: 300–800 ms reads, ~15% failure, seed
  data, error taxonomy), the UI state model (§9), architecture constraints (§10).
- `docs/BACKLOG.md` — epics `E0`–`E8`, task ids `TB-xxx`, iteration plan and cut-line.

Requirement ids are stable; do not renumber them. Note §8 records that the planning comment inside
`FakeTaskApi.kt` (200–900 ms, 1-in-7) is **wrong** and superseded by the spec — fixing it is `TB-101`.

## Commands

```bash
./gradlew :app:assembleDebug     # compiles the whole graph
./gradlew test                   # all unit tests
./gradlew lint                   # checkDependencies = true, so it follows module dependencies
./gradlew installDebug           # onto a connected device or emulator
./gradlew projects               # module graph
```

Single module / single test — Android library modules build variants, pure-JVM `:lib:*` modules do not:

```bash
./gradlew :feature:task-list:testDebugUnitTest --tests "*TaskListViewModelTest"
./gradlew :lib:tasks-api:test --tests "*SaveTaskUseCaseTest"
```

On Windows use `gradlew.bat` from PowerShell (`./gradlew` works from the Bash tool). Gradle 9.6 with
configuration cache and parallel builds on; daemon toolchain is JDK 25, modules compile to JVM 17.

## Architecture

Nine modules. The layering is **enforced by the build**, not by convention — a violating import does
not compile:

```
                        :app
                          │  composition root: NavHost + the only module that sees :data
        ┌─────────────────┼──────────────────┬──────────────────┐
:feature:task-list  :feature:task-editor  :data:tasks   (binds contracts → impls)
        └────────┬────────┘                  │
                 ▼                           ▼
          :core:ui  :core:common ──────► :lib:tasks-api
                 └──────────────────────► :lib:navigation-api
```

| Module | May depend on |
|---|---|
| `:lib:*` | nothing — pure Kotlin JVM, so `import android.*` in the domain fails to compile |
| `:core:common` | `:lib:*` |
| `:core:ui` | `:core:common`, `:lib:tasks-api` |
| `:feature:*` | `:core:*`, `:lib:*` — never another feature, never `:data:*` |
| `:data:*` | `:lib:*`, `:core:common` — never a feature, never `:core:ui` |
| `:app` | everything; the only place a contract is bound to an implementation |

Consequences that shape every change:

- Anything two features both need is a `:core` or `:lib` change, never a feature-to-feature import.
- Features resolve `TaskRepository` (from `:lib:tasks-api`) and can never name `DefaultTaskRepository`
  or `FakeTaskApi` — those are bound in `data/tasks/.../di/DataModule.kt`, installed via `:app`.
- Adding a field to `Task` ripples through `:data:tasks` and both features; budget accordingly.

## Conventions to follow when adding code

**Screens are split `…Route` / `…Screen`.** `TaskListRoute` is the only place that knows a ViewModel
exists (`hiltViewModel()`, `collectAsStateWithLifecycle()`); `TaskListScreen` is stateless, previewable
and testable without Hilt. Keep new screens in that shape.

**Navigation is type-safe and feature-owned.** Destinations are `Route` in `:lib:navigation-api`; each
feature exposes a `NavGraphBuilder.xxxSection(...)` extension that `TodoNavHost` composes. `:app` never
imports a screen composable, and features never import each other. Route args carry an **id, not an
object** — the editor refetches from `SavedStateHandle`.

**UI state is one immutable data class, not a sealed hierarchy** (a refresh must spin while loaded rows
stay on screen). Derived flags like `isEmpty` are computed properties, never stored. Render precedence
for the list is fixed in `docs/REQUIREMENTS.md` §9.

**Errors are typed and string-free.** The data layer always fails with `DataException(DataError)`;
transport types never escape `:data:tasks`. Map a `Throwable` with `asDataError()`. All wording lives in
`core/ui/.../error/DataErrorMessages.kt` — never put strings in `DataError`.

**Wrap data-layer calls in `suspendRunCatching`**, not `runCatching`, which swallows
`CancellationException`.

**Inject the ambient world.** Dispatchers come from `@IoDispatcher` / `@DefaultDispatcher`
(`:core:common`), timestamps from `Clock`, and the mock layer's `Random` must be injectable and seedable
— tests must never depend on the dice (NFR-05). The mock failure rate is meant to be a `var` so a demo
switch can zero it.

**Test doubles live in `:core:testing`** (`FakeTaskRepository`, `TestData`, `MainDispatcherRule`) and are
wired as `testImplementation` by the feature convention plugin. A feature ViewModel is tested against
`FakeTaskRepository` with no data module on the classpath. Test stack: JUnit4, coroutines-test, Turbine,
MockK.

## Build setup

Convention plugins in `build-logic/convention/` carry all shared Android config, so a module's build file
is a plugin id plus a namespace:

| Plugin id | Use for |
|---|---|
| `todo.android.library` | baseline Android module: SDK levels, JVM 17, lint, unit-test defaults |
| `todo.android.library.compose` | the above + Compose |
| `todo.android.hilt` | Hilt + KSP |
| `todo.android.feature` | a screen module: compose + hilt + `:core:*`/`:lib:*` deps + `:core:testing` |
| `todo.jvm.library` | pure Kotlin JVM (`:lib:*`) |

SDK levels live only in `AndroidSdk` (`build-logic/.../convention/ProjectExtensions.kt`): minSdk 26,
compile/target 37. Convention plugins cannot use `libs.` accessors — look the catalog up via the
`Project.libs` extension and `libs.findLibrary("...")`. Versions are centralized in
`gradle/libs.versions.toml` (AGP 9.4.0, Kotlin 2.3.20, Hilt 2.60.1, Compose BOM 2026.03.01).

A new feature module needs: `include(":feature:x")` in `settings.gradle.kts`, a build file with
`id("todo.android.feature")` + `namespace`, a nav section extension, and a line in `:app`'s dependencies
and `TodoNavHost`.
