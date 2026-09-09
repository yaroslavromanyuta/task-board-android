# Test plan

Fifty-four journeys, 539 steps. Run instructions are in [`README.md`](README.md); this file is the
catalogue, the traceability matrix and the list of things expected to fail.

All fifty-four have been run. Eight of them have since been rewritten: AX-03 failed on a defect it
found, and seven more passed by asserting behaviour that has now been ruled wrong and fixed — issues
#12 to #18, closed together. **Those eight were re-run on the device against the fixes and all eight
pass**; the other 46 passed on the build before the fixes and were not re-run. The seventeen journeys
added by the review of 2026-09-09 are marked 🆕 in the catalogue below.

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
- the in-memory store's cold-start boundary;
- the failure half of every write — a delete, an undo and a save that do not land;
- the window *inside* a call, which a test dispatcher closes before a second tap can reach it;
- the back stack: a screen left during a save, and an offer left behind on another screen.

Two areas are excluded on purpose. `DataError.Timeout` and `DataError.Conflict` have no scenarios:
`BACKLOG.md:295` closes open question Q-3 by recording that nothing emits them, and a journey there
would be testing dead taxonomy. `DataError.NotFound` was excluded with them until the review found a
route to it that a user can walk — an editor restored over an id the restarted source never issued —
so it now has ED-09 and the taxonomy is two-thirds covered rather than half. And nothing here
re-asserts a pure sorting or trimming rule that `TaskSortTest` or `SaveTaskUseCaseTest` already pins
at the unit level — only the observable end of those rules appears, and only where the wiring between
layers is what is in doubt.

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

### TL — the list (21)

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
| TL-09 | A second delete does not replace the first undo offer | FR-12 🔧 |
| TL-10 | Undo under an active search restores outside the filter | FR-10 + FR-12 |
| TL-11 | Search matches titles only, case-insensitively | FR-10 |
| TL-12 | Sorting reorders the list and composes with search | FR-11 |
| TL-13 | An undo that fails offers the task again | FR-12 🆕 🔧 |
| TL-14 | A delete that fails keeps the row and offers no undo | FR-04 🆕 |
| TL-15 | A restore that fails halfway says which field it lost | FR-12 🆕 🔧 |
| TL-16 | A failure arriving over a pending undo waits its turn | FR-12 🆕 🔧 |
| TL-17 | An undo offer survives a trip to the editor and back | FR-12 🆕 |
| TL-18 | Deleting the only match shows the no-matches copy, not the empty state | FR-08 + FR-10 🆕 |
| TL-19 | A task created under an active query is brought into view | FR-02 + FR-10 🆕 🔧 |
| TL-20 | A double-tapped row opens one editor, not two | FR-05 🆕 |
| TL-21 | A checkbox is inert while its own write is in flight | FR-03 🆕 🔧 |

TL-06 and TL-07 are the two halves of the undo guarantee that cost the most to get right. `TaskApi`
has no restore operation, so `RestoreTaskUseCase` re-creates the task — which means a new id, a new
position at the bottom of the list, and a second `setCompleted` call for a task that was completed.
TL-06 pins the position change as intended behaviour rather than a bug; TL-07 catches the dropped
second call, the failure mode where a completed task quietly returns unfinished. TL-15 is the third
side of the same triangle: the two calls still are not atomic — that needs a restore operation the
source does not have — so what it now pins is that a half-succeeded restore says so, in a sentence
about the flag rather than a bare network error over a row that visibly arrived.

TL-09, TL-13 and TL-16 are three routes to one rule, and they moved together when it changed: a
`TaskDeleted` message holds the only copy of a deleted task, so nothing may displace one that has not
been acted on. A second delete used to overwrite it, an unrelated failure used to overwrite it, and a
failed undo used to spend it. The state holds a queue now, and each of the three asserts its own way
in.

TL-13, TL-14 and TL-16 close the suite's largest gap. Every write in the app had a happy path on a
device and no failing one except the toggle in ST-04 — yet ~15% is the shipped failure rate, so the
failing path is what one user in seven meets. Each of the three is a different consequence: a delete
that does not happen, an undo that does not happen, and an unrelated failure landing on top of an
undo offer.

TL-20 and TL-21 both live inside a call. They exist because a test dispatcher closes that window
before a second tap can land in it: on a device, two taps in one frame are ordinary, and the second
one either pushes a second editor or lands on a control whose write is still out.

### ED — the editor (10)

| Id | Scenario | Covers |
|---|---|---|
| ED-01 | The create form opens empty with Save disabled | FR-02 |
| ED-02 | A whitespace-only title never reaches the validation error | FR-02 ⚠ |
| ED-03 | Titles are trimmed and empty notes are dropped | FR-02 |
| ED-04 | Opening a task seeds the form and an edit reaches the list | FR-05 |
| ED-05 | Leaving the editor discards an unsaved edit without asking | FR-05 ⚠ |
| ED-06 | A failed save keeps the form and does not navigate | FR-02 |
| ED-07 | A failed edit-mode load replaces the form and retries | FR-09 |
| ED-08 | A double-tapped Save creates one task, not two | FR-02 🆕 |
| ED-09 | Saving a task the source has forgotten reports NotFound over the form | FR-09, §8 taxonomy 🆕 |
| ED-10 | A save already in flight survives leaving the editor | FR-02 🆕 🔧 |

ED-06 and ED-07 are the pair that proves the editor's three failure fields are actually three. A
failed *save* must leave the form untouched under a snackbar; a failed *load* has no form worth
protecting, so it becomes the screen. Collapsing the two is the obvious refactor and the one that
loses the user's typing. ED-07 now taps Retry as well as observing it: the journey was named for a
retry it never performed, so the editor's recovery path had no on-device coverage at all.

ED-08 and ED-10 are the two ends of an asynchronous save, and both are now guarded. `onSave` returns
early while `isSaving`, and the call itself runs on an application-scoped coroutine, so popping the
editor no longer cancels a write the user had already committed to — only the reporting of it is
lifecycle-bound. ED-09 is what makes `DataError.NotFound` reachable without inventing a scenario: the ids the
source hands out restart with the process, and a restored editor can outlive the id it holds.

### DD — due dates (6)

| Id | Scenario | Covers |
|---|---|---|
| DD-01 | Due dates render in human terms and overdue ones stand out | FR-13 |
| DD-02 | The editor can set, change and clear a due date | FR-13 |
| DD-03 | Cancelling the date picker leaves the date untouched | FR-13 |
| DD-04 | The relative wording stops at seven days | FR-13 |
| DD-05 | Today and tomorrow are worded, not counted | FR-13 🆕 |
| DD-06 | The relative wording stops seven days into the past as well | FR-13 🆕 |

The seed rows only ever render "in 3 days", "yesterday" and no date at all, so "Due today" and "Due
tomorrow" — two separate string lookups, not the plural the counted cases use — had never been on
screen. DD-06 walks the negative half of a boundary that is written twice in `relativeDateOf`, once
per direction.
### LC — lifecycle (6)

| Id | Scenario | Covers |
|---|---|---|
| LC-01 | Rotating the list keeps its query and sort | NFR-06 |
| LC-02 | Rotating the editor keeps a half-filled form | NFR-06 |
| LC-03 | The date picker survives a rotation | NFR-06 🔧 |
| LC-04 | A half-typed form survives process death | NFR-07 |
| LC-05 | A cold start returns to the four seed rows | D-2 |
| LC-06 | An edited form survives process death and is not reloaded over | NFR-07 🆕 |

LC-05 records a deviation rather than a defect, and it is also the suite's own foundation: every
other journey depends on `force-stop` restoring the seed rows exactly.

LC-04 kills the process over a create form, where there is nothing older to lose. LC-06 is the half
that has something to lose: an edit form has a task behind it, and `if (!hasSavedForm) taskId?.let(::load)`
is the one line standing between the user's typing and the source's older copy of it.

### AX / TH — accessibility and theme (5)

| Id | Scenario | Covers |
|---|---|---|
| AX-01 | Every icon-only control carries a description | NFR-09 |
| AX-02 | The row checkbox announces the action its tap performs | NFR-09 🔧 |
| TH-01 | Dark mode keeps the priority colours apart | FR-14, TB-705, TB-706 |
| AX-03 | The list and the form stay usable at 200% font scale | NFR-09, FR-01 🆕 🔧 |
| TH-02 | Switching to dark mode keeps a half-filled form | NFR-06, FR-14 🆕 |

Both AX-01 and AX-02 cover ground that `BACKLOG.md` task TB-308 verified by reading the code. This is
the first time either is checked against what the system actually exposes.

AX-03 is the first journey that scrolls anything. FR-01 asks for a scrollable list and the four seed
rows never fill a screen — not even at 200%, which the run measured — so the journey adds two rows of
its own to overflow the viewport. That is what turned up issue #12: the list scrolled, and the row it
scrolled to had a Delete control the FAB was sitting on. Its last action is now the regression guard
over the bottom content padding that fixed it. TH-02 covers the configuration change users
actually perform — NFR-06 says configuration change, and LC-01 to LC-03 only ever rotate.

## Suites

**Smoke — 8 journeys, roughly ten minutes.** Enough to say the build is worth testing further; it
touches both screens, both failure channels, navigation and a configuration change.

```
ST-01  ST-04  TL-01  TL-05  TL-06  ED-01  ED-04  LC-01
```

**Full — all 54.** Force-stop between each one. The last full run was against the build before issues
#12 to #18 were fixed. The eight journeys those fixes touched were rewritten and re-run on 2026-09-09
and pass; the other 46 have not been re-run since the fixes landed.

**Write-failure set — 6 journeys.** The failing half of every write, which the suite had almost none
of before the review. Worth running as a block, because they share the broadcast recipe and each one
needs the source broken at a different moment.

```
TL-13  TL-14  TL-15  TL-16  ED-09  ED-10
```

⚠ marks a journey that asserts behaviour someone still has to rule on; 🔧 marks one that has been
rewritten as a regression guard over a fix — either because it failed, or because it passed by
asserting what was then judged wrong; 🆕 marks one added on 2026-09-09.

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

### 2026-09-09 — suite review, seventeen journeys added

No device time. A read of the 37 journeys against the source and `REQUIREMENTS.md` looking for corner
cases nothing asserts. Six gaps, in rough order of what they would cost a user:

1. **The failing half of a write was almost uncovered.** ST-04 breaks a toggle; nothing broke a
   delete, an undo or a create. At the shipped 15% these are not edge cases — they are one user in
   seven. Added TL-13, TL-14, TL-15, TL-16, ED-09, ED-10.
2. **Nothing acted inside a call.** Every journey either pinned the latency to read a screen or let
   the call finish. The second tap, the Back press mid-save, the interleaving that a test dispatcher
   cannot produce — none of it was reachable. Added ED-08, ED-10, TL-20, TL-21.
3. **ED-07 never tapped Retry.** The journey is named for a retry, the traceability matrix counts it
   under FR-09 recovery, and the actions stop at Back. Extended in place rather than added, since the
   id already claimed the coverage.
4. **`hasNoMatches` and the query had one path in and none out.** Nothing deleted the last matching
   row, and nothing created a task the live query hides — the second of which looks exactly like a
   failed save. Added TL-18, TL-19.
5. **Two relative-date branches had never rendered, and one snackbar had never crossed a screen.**
   "Due today", "Due tomorrow" and the whole negative half of the boundary; and an undo offer left
   behind by navigating. Added DD-05, DD-06, TL-17.
6. **Two configuration facts went untested.** FR-01 asks for a scrollable list that four rows never
   fill, and NFR-06 says configuration change while every journey only rotated. Added AX-03, TH-02.

Six of the new journeys assert behaviour that is arguably wrong and are on the watchlist below. The
other eleven were expected to pass. Ten did.

### 2026-09-09 — the added journeys, run

Same device as the 2026-09-08 runs: Samsung SM-G973F (`RF8M32EDAQD`), Android 12 / API 31, animations
off. Build: `:app:installDebug` at commit `68c0479`. Results in
[`results/2026-09-09-new-journeys-run.json`](results/2026-09-09-new-journeys-run.json).

**17 of 18 passed. 208 actions, 207 passed, 1 failed, none skipped.** The eighteenth is ED-07, whose
retry path was added rather than written fresh.

The other 36 journeys were not re-run. Nothing in this change touches app code, and they passed on
this build on 2026-09-08.

| Failed | Why |
|---|---|
| **AX-03** ❌ #12 | **New defect.** At the last action: the bottom row's Delete control is not in the layout dump at all. The FAB is drawn over that corner and the list has no bottom content padding, so scrolling cannot clear it. Reproduced at font scale 1.0 with ten rows, so it is not a 200% artefact |

**Six watchlist predictions were confirmed on the device**, which is what their journeys passing
means: TL-13, TL-15, TL-16, TL-19, TL-21 and ED-10. Each is now a filed defect rather than a
prediction — issues #13 to #18.

Three things about running these are worth keeping:

- **The undo window is ten seconds and a layout dump costs three.** TL-17 lost its snackbar to two
  dumps taken between returning to the list and tapping Undo; the tap then landed on the empty list
  and the task stayed deleted. Read once, then act.
- **TL-15's window is narrower still.** The broadcast has to land after `createTask` has rolled its
  dice and before `setCompleted` rolls its own. Two attempts missed it in both directions — the
  create failed, then both calls succeeded — before it reproduced. Issue the tap and the broadcast in
  one `adb shell` line with a device-side `sleep` between them.
- **A selected chip's `checked` is on the wrapping `View`.** `qa/README.md` says a `FilterChip`
  reports `checked="true"`; on this device the `CheckBox` child reads `false` and its parent `View`
  reads `true`. Match the parent.

### 2026-09-09 — issues #12 to #18 fixed

No device time. All seven open defects closed in one change, and the eight journeys that encoded the
old behaviour rewritten as regression guards: AX-03, TL-09, TL-13, TL-15, TL-16, TL-19, TL-21, ED-10.
TL-09 was closed by the same change as TL-16 without ever being filed as an issue of its own.

`./gradlew test lint` green: 86 unit tests, no lint errors - nine more than before, plus two rewritten
where the old assertion was the defect. But four of the seven fixes are only partly visible below the
device - the FAB overlap and the disabled control are Compose-layer, the back-stack result is
navigation, and the cancelled save can only be approximated by cancelling `viewModelScope` by hand.
So the eight journeys were run on the device the same day; that run is the section below, and it
found a defect in one of the fixes.

What changed, per issue:

| Issue | Fix |
|---|---|
| #12 | `contentPadding = PaddingValues(bottom = 88.dp)` on the list, so the FAB stops covering the last row's Delete |
| #13 | `onUndoDelete` re-posts the offer when the restore fails, instead of spending it before the call |
| #14 | `RestoreTaskUseCase` returns a `RestoreOutcome`, so a re-created task whose flag did not land is reported as a partial restore and worded as one |
| #15 | `TaskListUiState.messages` is a queue; nothing displaces a `TaskDeleted` that has not been acted on. Closes TL-09 as well |
| #16 | The editor reports a successful save back through the back stack entry; `taskListSection` reads it there and the list clears its query, so the new task is on screen |
| #17 | The row is held for the duration of its completion write and the checkbox is disabled, the shape `TaskEditorViewModel.onSave` already used |
| #18 | The save runs on an `@ApplicationScope` coroutine and is only awaited from `viewModelScope`, so leaving the editor no longer cancels it |

### 2026-09-09 — the eight fix journeys, run

Same device as every other run: Samsung SM-G973F (`RF8M32EDAQD`), Android 12 / API 31, animations
off. Build: `:app:installDebug` from the working tree carrying the fixes. Results in
[`results/2026-09-09-fix-verification.json`](results/2026-09-09-fix-verification.json).

**8 of 8 pass**, after one of them failed and sent a fix back for rework.

| Id | Result | Note |
|---|---|---|
| AX-03 | PASSED | The bottom row's Delete is in the dump at `[965,1770][1028,1833]` and the FAB sits entirely below it. This journey failed on the previous build |
| TL-09 | PASSED | Both offers honoured, in order. Needed a second attempt for timing — see below |
| TL-13 | PASSED | The offer returns behind the failure, and the second Undo restores the task with its completion flag |
| TL-15 | PASSED | First attempt. "Task restored, but it came back unfinished." over an unchecked, unstruck row |
| TL-16 | PASSED | The offer survives an unrelated failure; the failure is then shown after it, not instead of it |
| TL-19 | **FAILED, then PASSED** | The defect below. Passes against the reworked fix |
| TL-21 | PASSED | `enabled="false"` on that row's checkbox alone while its write is out; the row toggles normally afterwards |
| ED-10 | PASSED | The task appears ten seconds after Back was tapped 0.3 s into a 5 s call |

**TL-19 failed, and the failure was real.** The first fix for #16 wrote the "a save landed" result
onto `NavBackStackEntry.savedStateHandle` in `TodoNavHost` and read it from a `SavedStateHandle`
injected into `TaskListViewModel`. Those are two different handles — the entry keeps its own under an
internal holder's key, and Hilt builds the ViewModel's from the same registry under the ViewModel's
key — so the write never arrived and the query survived the save exactly as before. The unit test
passed because it constructed the ViewModel with the handle it then wrote into, which made them the
same object by construction; the defect lives in the wiring between the back stack and Hilt, which no
JVM test instantiates. `taskListSection` now reads the result off the entry, collects it as state and
hands it to `TaskListRoute`, which calls `TaskListViewModel.onTaskSaved()`; the ViewModel takes no
`SavedStateHandle` at all.

That is the whole case for this run. Seven of the eight journeys confirmed what the unit tests already
claimed. The eighth is the one that could not be checked below the device, and it was wrong.

Two things about running these are worth keeping:

- **A dump costs 3 s of a 10 s snackbar.** TL-09 and TL-13 each failed once purely on that: host round
  trips plus one `uiautomator dump` inside the undo window spent it, and the offer expired before the
  tap. Issue the taps as one `adb shell` line with device-side `sleep`s and dump only at the end.
- **`README.md` is stale about snackbar duration.** It says a failure snackbar is
  `SnackbarDuration.Short`. `TaskListScreen` passes `Long` for every message kind since the fix for
  TL-08, so both last about ten seconds — which is what makes TL-13's "wait for the failure to expire,
  then read the offer" step land at +11 s rather than +5 s.

## Known-defect watchlist

Thirteen entries. Five were written in advance as journeys asserting behaviour that may well be
wrong; TL-08 and AX-03 were found by running them; the other six were written by the 2026-09-09
review and confirmed on the device the same day.

**Eleven are now fixed** — TL-08 (#7), LC-03 (#8) and AX-02 (#9) on 2026-09-08, and AX-03 (#12),
TL-13 (#13), TL-15 (#14), TL-16 (#15), TL-19 (#16), TL-21 (#17), ED-10 (#18) and TL-09 on 2026-09-09.
Their journeys are rewritten as regression guards rather than defect probes. They are kept in the
table because the reasoning is worth not losing. The eight from 2026-09-09 have not been re-run on a
device.

TL-09 never had an issue of its own: it is the same single message slot as TL-16, reached by
deleting twice instead of by losing a dice roll, and the queue closed both.

**Two remain open.** ED-05 and ED-02 assert the current behaviour, so they pass; passing *is* the
finding, and each names a trade-off that is product's call rather than QA's.

| Id | What the journey found | Why it matters | The fix |
|---|---|---|---|
| **TL-08** ✅ #7 | *Fixed.* The undo snackbar never went away. `TaskListScreen.kt:168-171` calls `showSnackbar(message, actionLabel)` with no `duration`, and Material3 defaults to `SnackbarDuration.Indefinite` whenever an `actionLabel` is present — so "Task deleted" sits over the list until the user taps Undo or swipes it off, and `withDismissAction` is false, so there is no × either | The failure snackbar next to it *is* `Short`, which is what makes this look unintended rather than chosen. It permanently occludes the bottom of the list, and `message` stays set in the state the whole time. Confirmed on device: still on screen after 25 s | Pass `duration = SnackbarDuration.Long` explicitly |
| **TL-09** ✅ | *Fixed with #15.* `TaskListUiState` held one nullable `message`, so a second delete overwrote the first `TaskDeleted` before the user could act on it | Undo is the only route back and it was offered exactly once. Delete two rows quickly and the first was unrecoverable, silently | `messages` is a queue; both offers are honoured, in the order they were made |
| **ED-05** ⚠ open | Back discards an unsaved edit with no prompt; `onBack` and `onDone` both go to `navigateUp` (`TodoNavHost.kt:31-32`) | Unlike a delete, this has no snackbar behind it. A long note typed and lost is gone with no trace | A "discard changes?" dialog when the form is dirty |
| **LC-03** ✅ #8 | *Fixed.* The picker's visibility was `remember`, not `rememberSaveable`, so a rotation closed it | NFR-06 asks for state intact across a configuration change. Whether an open dialog counts is the open question | One word: `rememberSaveable` |
| **AX-02** ✅ #9 | *Fixed.* The row checkbox was described as "Mark complete" whatever its state | TalkBack tells a user they can complete a task that is already complete. The checked state is exposed correctly, so the label contradicts it | A second string, chosen on `isCompleted` |
| **ED-02** ⚠ open | `canSave` is already false for a whitespace-only title, so `SaveTaskUseCase` is never reached and "A title is required." cannot appear | Not a functional break — the rule holds. But a string, a `supportingText` branch and a tested ViewModel path are unreachable dead UI | Either drop the inline error, or let Save through and rely on the use case |
| **TL-13** ✅ #13 | *Fixed.* `onUndoDelete` cleared `message` before it called `restoreTask`, so a restore that failed left a network snackbar and no offer | Undo is the only route back and it was spent whether or not it worked. At 15% that lost a task outright about one undo in seven | The offer is re-posted when the restore fails, behind the failure that explains it |
| **TL-15** ✅ #14 | *Fixed, in the reporting rather than the mechanism.* `RestoreTaskUseCase` is still `createTask` then `setCompleted` and the pair is still not atomic — that needs a restore operation on `TaskApi` | The row is on screen and looks restored. The only signal that it was not used to be a network snackbar that said nothing about completion | `RestoreOutcome.CompletionLost` and a sentence that names the flag: "Task restored, but it came back unfinished." |
| **TL-16** ✅ #15 | *Fixed.* Any `reportFailure` overwrote a pending `TaskDeleted` in the same single `message` slot — a background toggle was enough | TL-09 needed the user to delete twice. This needed them to do nothing at all: one lost dice roll took the undo away | A queue, and the rule that nothing displaces an offer that has not been acted on. The failure is still shown, after it |
| **TL-19** ✅ #16 | *Fixed, at the second attempt.* A save returned to a list still filtered by the query, so a new task whose title did not match was created and invisible | The editor closing is the app's only "saved" signal, and the list then showed no such task — the same picture a failed save paints | The editor reports the save back through the back stack entry, and `taskListSection` reads it *there*: the first fix read it from a `SavedStateHandle` injected into the ViewModel, which is a different handle, and the device run caught it |
| **TL-21** ✅ #17 | *Fixed.* The checkbox renders the cache, which does not move until the write lands, so a second tap during the first write sent "complete" twice | Two taps read as complete-then-undo and left the task complete. Nothing was reported, and the second tap was simply absorbed | The row is held for the duration of its write and the control is disabled — the shape `onSave` already used. The second tap still does not toggle back; it no longer looks as though it should |
| **ED-10** ✅ #18 | *Fixed.* Popping the editor cleared the ViewModel, cancelled `viewModelScope` and with it the in-flight `createTask` | The user tapped Save and left, which reads as saved. Unlike a delete there is no snackbar behind it and no trace afterwards | The write runs on an `@ApplicationScope` coroutine and is only awaited from `viewModelScope` |
| **AX-03** ✅ #12 | *Fixed.* The `LazyColumn` had no bottom `contentPadding`, so the last row ended flush with the viewport and the FAB was drawn over its right-hand end. The Delete control was not merely covered — it was absent from the layout dump | The only way to delete the bottom task was to add another one, sort it away or search for it. Reproduced at the default font scale with ten rows, so any list long enough to scroll had one unreachable row | `contentPadding = PaddingValues(bottom = 88.dp)` on the list |

TL-08 was not predicted at all. It failed on an assumption its own journey made about how a
snackbar behaves — and the assumption turned out to be right about snackbars and wrong about this
one, which made it a defect in the app rather than in the journey. That is the case for writing the
obvious assertion down even when it looks too obvious to fail.

## Traceability

Every functional requirement now has at least one on-device scenario, and every write has both a
happy and a failing one.

| Requirement | Journeys |
|---|---|
| FR-01 scrollable list, long titles | TL-01, TL-02, AX-03 (the only one that scrolls) |
| FR-02 add a task | ED-01, ED-02, ED-03, ED-06, ED-08, ED-10, TL-19 |
| FR-03 toggle complete | TL-03, TL-04, TL-21 |
| FR-04 delete | TL-05, ST-05, TL-14 |
| FR-05 edit round trip | ED-04, ED-05, TL-20 |
| FR-06 everything through the mock source | implicit in all 37 — every assertion is downstream of a `TaskApi` call |
| FR-07 loading state | ST-01 |
| FR-08 empty state | ST-05, TL-18 |
| FR-09 error state and recovery | ST-02, ST-03, ST-04, ED-07, ED-09 |
| FR-10 search | ST-05, ST-06, TL-10, TL-11, TL-18, TL-19 |
| FR-11 sort | TL-12 |
| FR-12 undo delete | TL-05, TL-06, TL-07, TL-08, TL-09, TL-10, TL-13, TL-15, TL-16, TL-17 |
| FR-13 due dates | DD-01, DD-02, DD-03, DD-04, DD-05, DD-06 |
| FR-14 light and dark themes | TH-01, TH-02 |
| NFR-02 300–800 ms reads | ST-01 |
| NFR-06 configuration change | LC-01, LC-02, LC-03, TH-02 (night mode, not rotation) |
| NFR-07 process recreation | LC-04, LC-06, ED-09 |
| NFR-09 accessibility | AX-01, AX-02, AX-03, TL-01 (priority as text) |
| D-2 in-memory store | LC-05, ED-09 (ids restart with the process) |
| §8 error taxonomy | `Network` throughout, `NotFound` in ED-09; `Timeout` and `Conflict` are unreachable by design |

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
- **TalkBack itself.** AX-01 checks that descriptions exist and AX-03 that a 200%-scaled screen stays
  operable; neither checks that linear navigation reaches everything in a sensible order, or that a
  row announces as one thing rather than four.
- **A live locale or time zone change.** DD-05 and DD-06 read relative dates in the device's own
  zone. Crossing midnight, or moving the device between zones with a due date on screen, is the one
  due-date rule still verified only by `RelativeDateTest`.
