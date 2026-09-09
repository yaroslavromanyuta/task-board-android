# Test plan

Thirty-seven journeys, 322 steps. Run instructions are in [`README.md`](README.md); this file is the
catalogue, the traceability matrix and the list of things expected to fail.

## What this suite is for, and what it deliberately leaves alone

The 77 JVM unit tests already cover the mock source, the repository, the use cases and both
ViewModels — including every branch of the §9 state table at the state level. Repeating that on a
device would be slow and would prove nothing new.

So these journeys target what a unit test structurally cannot reach:

- the §9 precedence table **as it renders**, not as a state object;
- real navigation and the back stack;
- snackbar interaction — an offer that expires, and one that gets replaced;
- the date picker, a dialog with its own lifecycle;
- rotation and process death against the real Android runtime;
- accessibility as the system actually exposes it;
- the palette, in both themes;
- the in-memory store's cold-start boundary.

Two areas are excluded on purpose. `DataError.Timeout` and `DataError.Conflict` have no scenarios:
`BACKLOG.md:295` closes open question Q-3 by recording that nothing emits them, and a journey there
would be testing dead taxonomy. And nothing here re-asserts a pure sorting or trimming rule that
`TaskSortTest` or `SaveTaskUseCaseTest` already pins at the unit level — only the observable end of
those rules appears, and only where the wiring between layers is what is in doubt.

## Catalogue

### ST — the §9 state model, rendered (6)

| Id | Scenario | Covers |
|---|---|---|
| ST-01 | The first load shows a loading state | FR-07, NFR-02 |
| ST-02 | A first load that fails becomes the screen | FR-09 |
| ST-03 | Retry recovers from the error screen | FR-09 |
| ST-04 | **A failure over a loaded list does not blank it** | FR-09 boundary |
| ST-05 | An emptied list shows the empty state and hides its own controls | FR-08, FR-10 |
| ST-06 | No search matches is a different sentence from no tasks | FR-10 |

ST-04 is the highest-value journey in the suite. It is the one rule `TaskListViewModel.reportFailure`
exists to hold — content on screen means a failure passes over it — and the one whose breakage users
notice immediately, because it takes their list away.

### TL — the list (12)

| Id | Scenario | Covers |
|---|---|---|
| TL-01 | The four seed rows render with title, priority and a complete control | FR-01 |
| TL-02 | A long title does not break the row layout | FR-01, TB-303 |
| TL-03 | Completing a task marks it and survives a refresh | FR-03 |
| TL-04 | Un-completing a task clears the strikethrough | FR-03 |
| TL-05 | Deleting a task removes it and announces it | FR-04, FR-12 |
| TL-06 | Undo restores the task, at the bottom, with a new id | FR-12 |
| TL-07 | Undoing a completed task brings it back completed | FR-12 |
| TL-08 | The undo offer expires and the task stays deleted | FR-12 🔧 |
| TL-09 | A second delete replaces the first undo offer | FR-12 ⚠ |
| TL-10 | Undo under an active search restores outside the filter | FR-10 + FR-12 |
| TL-11 | Search matches titles only, case-insensitively | FR-10 |
| TL-12 | Sorting reorders the list and composes with search | FR-11 |

TL-06 and TL-07 are the two halves of the undo guarantee that cost the most to get right. `TaskApi`
has no restore operation, so `RestoreTaskUseCase` re-creates the task — which means a new id, a new
position at the bottom of the list, and a second `setCompleted` call for a task that was completed.
TL-06 pins the position change as intended behaviour rather than a bug; TL-07 catches the dropped
second call, the failure mode where a completed task quietly returns unfinished.

### ED — the editor (7)

| Id | Scenario | Covers |
|---|---|---|
| ED-01 | The create form opens empty with Save disabled | FR-02 |
| ED-02 | A whitespace-only title never reaches the validation error | FR-02 ⚠ |
| ED-03 | Titles are trimmed and empty notes are dropped | FR-02 |
| ED-04 | Opening a task seeds the form and an edit reaches the list | FR-05 |
| ED-05 | Leaving the editor discards an unsaved edit without asking | FR-05 ⚠ |
| ED-06 | A failed save keeps the form and does not navigate | FR-02 |
| ED-07 | A failed edit-mode load replaces the form and retries | FR-09 |

ED-06 and ED-07 are the pair that proves the editor's three failure fields are actually three. A
failed *save* must leave the form untouched under a snackbar; a failed *load* has no form worth
protecting, so it becomes the screen. Collapsing the two is the obvious refactor and the one that
loses the user's typing.

### DD — due dates (4)

| Id | Scenario | Covers |
|---|---|---|
| DD-01 | Due dates render in human terms and overdue ones stand out | FR-13 |
| DD-02 | The editor can set, change and clear a due date | FR-13 |
| DD-03 | Cancelling the date picker leaves the date untouched | FR-13 |
| DD-04 | The relative wording stops at seven days | FR-13 |

### LC — lifecycle (5)

| Id | Scenario | Covers |
|---|---|---|
| LC-01 | Rotating the list keeps its query and sort | NFR-06 |
| LC-02 | Rotating the editor keeps a half-filled form | NFR-06 |
| LC-03 | The date picker survives a rotation | NFR-06 🔧 |
| LC-04 | A half-typed form survives process death | NFR-07 |
| LC-05 | A cold start returns to the four seed rows | D-2 |

LC-05 records a deviation rather than a defect, and it is also the suite's own foundation: every
other journey depends on `force-stop` restoring the seed rows exactly.

### AX / TH — accessibility and theme (3)

| Id | Scenario | Covers |
|---|---|---|
| AX-01 | Every icon-only control carries a description | NFR-09 |
| AX-02 | The row checkbox announces the action its tap performs | NFR-09 🔧 |
| TH-01 | Dark mode keeps the priority colours apart | FR-14, TB-705, TB-706 |

Both AX journeys cover ground that `BACKLOG.md` task TB-308 verified by reading the code. This is
the first time either is checked against what the system actually exposes.

## Suites

**Smoke — 8 journeys, roughly ten minutes.** Enough to say the build is worth testing further; it
touches both screens, both failure channels, navigation and a configuration change.

```
ST-01  ST-04  TL-01  TL-05  TL-06  ED-01  ED-04  LC-01
```

**Full — all 37.** Force-stop between each one. All 37 pass on the current build, confirmed by a second full run after the fixes.

⚠ marks a journey that asserts behaviour someone still has to rule on; 🔧 marks one that was failing and is now a regression guard over a fix.

## Run log

### 2026-09-08 — smoke suite, first run

Device: Samsung SM-G973F (`RF8M32EDAQD`), Android 12 / API 31, 1080x2280 @ 420 dpi, animations off.
Build: `:app:installDebug` at commit `d972533`.

**8 of 8 passed.** No defects found by the smoke set; every scenario behaved exactly as written.

| Id | Result | Note |
|---|---|---|
| ST-01 | PASSED | `Loading` present, no rows, no error, search field absent - all six assertions |
| ST-04 | PASSED | The checkbox tap failed under the broken source, the snackbar appeared, all four rows stayed, no Retry. The checkbox also stayed unchecked, which is the cache-on-success rule (Q-2) holding |
| TL-01 | PASSED | Four rows in seed order; 4x Mark complete, 4x Delete task, 1x High, 2x Medium, 1x Low |
| TL-05 | PASSED | Row gone, "Task deleted" with "Undo", three delete icons left |
| TL-06 | PASSED | Restored **at the bottom**, below the long row, with High and "Due in 3 days" intact - the new-id behaviour the journey predicts |
| ED-01 | PASSED | Empty form, Medium pre-selected, Save `enabled="false"` until a title exists, then saves and returns |
| ED-04 | PASSED | Form seeded with title, notes, High and "Due in 3 days"; edit to "Renew the domain" + Low reached the list and held its position |
| LC-01 | PASSED | Query "e" and the Priority sort both survived portrait -> landscape -> portrait, with no reload |

Three findings about the tooling came out of this run and are written up in `README.md` rather than
here, because they change how every future run is executed: `android layout` takes about 20 s per
call, disabled state lives on the clickable parent rather than on the label, and a selected
`FilterChip` reports `checked` rather than `selected`.

### 2026-09-08 — full suite

Same device and build. Results are in
[`results/2026-09-08-full-run.json`](results/2026-09-08-full-run.json), per action, in the format the
journey contract specifies.

**34 of 37 passed. 309 actions passed, 3 failed, 5 skipped** (evaluation ends at a journey's first
failing action).

| Failed | Why |
|---|---|
| **TL-08** | **New defect.** The undo snackbar never expires — see the watchlist below |
| **LC-03** ✅ #8 | Predicted. The date picker is dismissed by rotation |
| **AX-02** ✅ #9 | Predicted. A completed row still reports `content-desc="Mark complete"` |

Three journeys needed a second attempt before the app's behaviour could be read at all, and none of
the three was an app fault:

- **ST-04** — the failure snackbar is `SnackbarDuration.Short`, about four seconds. A dump started
  five seconds after the tap saw nothing. Read promptly, every assertion holds.
- **TL-10** — the soft keyboard covers the snackbar. The first attempt tapped through it and hit
  backspace instead of Undo, which cleared the query and left the task deleted. Dismiss the keyboard
  before touching a snackbar.
- **DD-04** — a reader that matches every string starting with `Due ` also picks up the section
  label `Due date`. Exclude it.

All three are now written up in `README.md`, because they change how any future run is executed.

Everything else passed first time, including all seven editor journeys and all four due-date ones.

### 2026-09-08 — fix verification

Same device. Results in
[`results/2026-09-08-fix-verification.json`](results/2026-09-08-fix-verification.json).

All three failing journeys pass against the fixes for issues #7, #8 and #9. **22 actions, all
passed**, including the regression checks each one carries beyond its own steps: Undo still works
inside the shortened snackbar window, Cancel still dismisses the picker, and unfinished rows still
read "Mark complete".

The snackbar now clears after about ten seconds — `SnackbarDuration.Long`, measured on device.

`./gradlew test lint` stayed green: 77 unit tests, no lint errors. None of the three fixes was
visible to a unit test, which is the point.

### 2026-09-08 — full suite, confirmation run against the fixed build

Same device. Results in
[`results/2026-09-08-full-run-after-fixes.json`](results/2026-09-08-full-run-after-fixes.json).

**All 37 journeys passed. 323 actions, none failed, none skipped.** TL-08, LC-03 and AX-02 — the
three that failed before — pass as regression guards.

The run also turned up something the fixes themselves had missed. Fixing #9 changed a string the
suite asserts: **TL-01** counted four checkboxes described "Mark complete", and **TL-04** tapped one
by that name on the row that arrives completed. Both now name "Mark incomplete" where the row is
done. Nothing was wrong with the app; the suite had encoded the old label in two places, and a
re-run is the only thing that would have said so.

Everything else passed unchanged.

## Known-defect watchlist

Six entries. Five were written in advance as journeys asserting behaviour that may well be wrong;
the sixth, TL-08, was found by running them.

**Three are now fixed** — TL-08 (#7), LC-03 (#8) and AX-02 (#9) — and their journeys have been
rewritten as regression guards rather than defect probes. They are kept in the table because the
reasoning is worth not losing.

**Three remain open.** TL-09, ED-05 and ED-02 assert the current behaviour, so they pass; passing
*is* the finding, and each names a trade-off that is product's call rather than QA's.

| Id | What the journey found | Why it matters | The fix |
|---|---|---|---|
| **TL-08** ✅ #7 | *Fixed.* The undo snackbar never went away. `TaskListScreen.kt:168-171` calls `showSnackbar(message, actionLabel)` with no `duration`, and Material3 defaults to `SnackbarDuration.Indefinite` whenever an `actionLabel` is present — so "Task deleted" sits over the list until the user taps Undo or swipes it off, and `withDismissAction` is false, so there is no × either | The failure snackbar next to it *is* `Short`, which is what makes this look unintended rather than chosen. It permanently occludes the bottom of the list, and `message` stays set in the state the whole time. Confirmed on device: still on screen after 25 s | Pass `duration = SnackbarDuration.Long` explicitly |
| **TL-09** ⚠ open | `TaskListUiState` holds one nullable `message`, so a second delete overwrites the first `TaskDeleted` before the user can act on it | Undo is the only route back and it is offered exactly once. Delete two rows quickly and the first is unrecoverable, silently | A queue of pending deletes, or a confirmation on the second |
| **ED-05** ⚠ open | Back discards an unsaved edit with no prompt; `onBack` and `onDone` both go to `navigateUp` (`TodoNavHost.kt:31-32`) | Unlike a delete, this has no snackbar behind it. A long note typed and lost is gone with no trace | A "discard changes?" dialog when the form is dirty |
| **LC-03** ✅ #8 | *Fixed.* The picker's visibility was `remember`, not `rememberSaveable`, so a rotation closed it | NFR-06 asks for state intact across a configuration change. Whether an open dialog counts is the open question | One word: `rememberSaveable` |
| **AX-02** ✅ #9 | *Fixed.* The row checkbox was described as "Mark complete" whatever its state | TalkBack tells a user they can complete a task that is already complete. The checked state is exposed correctly, so the label contradicts it | A second string, chosen on `isCompleted` |
| **ED-02** ⚠ open | `canSave` is already false for a whitespace-only title, so `SaveTaskUseCase` is never reached and "A title is required." cannot appear | Not a functional break — the rule holds. But a string, a `supportingText` branch and a tested ViewModel path are unreachable dead UI | Either drop the inline error, or let Save through and rely on the use case |

TL-08 was not predicted at all. It failed on an assumption its own journey made about how a
snackbar behaves — and the assumption turned out to be right about snackbars and wrong about this
one, which made it a defect in the app rather than in the journey. That is the case for writing the
obvious assertion down even when it looks too obvious to fail.

## Traceability

Every functional requirement now has at least one on-device scenario.

| Requirement | Journeys |
|---|---|
| FR-01 scrollable list, long titles | TL-01, TL-02 |
| FR-02 add a task | ED-01, ED-02, ED-03, ED-06 |
| FR-03 toggle complete | TL-03, TL-04 |
| FR-04 delete | TL-05, ST-05 |
| FR-05 edit round trip | ED-04, ED-05 |
| FR-06 everything through the mock source | implicit in all 37 — every assertion is downstream of a `TaskApi` call |
| FR-07 loading state | ST-01 |
| FR-08 empty state | ST-05 |
| FR-09 error state and recovery | ST-02, ST-03, ST-04, ED-07 |
| FR-10 search | ST-05, ST-06, TL-10, TL-11 |
| FR-11 sort | TL-12 |
| FR-12 undo delete | TL-05, TL-06, TL-07, TL-08, TL-09, TL-10 |
| FR-13 due dates | DD-01, DD-02, DD-03, DD-04 |
| FR-14 light and dark themes | TH-01 |
| NFR-02 300–800 ms reads | ST-01 |
| NFR-06 configuration change | LC-01, LC-02, LC-03 |
| NFR-07 process recreation | LC-04 |
| NFR-09 accessibility | AX-01, AX-02, TL-01 (priority as text) |
| D-2 in-memory store | LC-05 |

Not covered here, and correctly so: NFR-01, NFR-04, NFR-05, NFR-08 and NFR-10 through NFR-13 are
properties of the source, the build or the module graph. They are verified by the compiler, by
`./gradlew lint` with `checkDependencies = true`, and by the existing unit tests — a device cannot
observe them, and a journey that claimed to would be lying.

## What would extend this suite

- **Compose UI tests under `src/androidTest`.** They would run in CI, which journeys cannot. The
  cost is real: `hilt-android-testing` and a `HiltTestApplication` runner replacing
  `app/build.gradle.kts:22`, `androidTestImplementation(ui-test-junit4)` added to
  `AndroidLibraryComposeConventionPlugin.kt`, and `testTag`s across roughly twenty composables.
  `WALKTHROUGH.md` §7 item 6 already names the landing site.
- **Screenshot tests.** TH-01 and DD-01 both end in "verify from a screenshot", which is where a
  human still has to look. Every screen already has a `@PreviewLightDark`, so Paparazzi or Roborazzi
  would turn those into assertions — §7 item 5.
- **Scale.** Nothing here goes past four rows. Paging is the seam (§7 item 4), and a fifty-row
  journey is the cheapest way to find out whether it is needed yet.
- **Font scale and TalkBack.** AX-01 checks that descriptions exist; it does not check that a
  200%-scaled row stays usable or that linear navigation reaches everything in a sensible order.
