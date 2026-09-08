# QA journey suite

Thirty-seven end-to-end scenarios for Task Board, written in the XML journey format and run against
a real device or emulator through the `android` CLI.

They exist because the 77 unit tests in this repo assert ViewModel *state* and never render a pixel.
Nothing below the ViewModel boundary — the §9 precedence table as it actually draws, real
navigation, snackbar interaction, the date-picker dialog, rotation, process death, the fact that an
in-memory store forgets everything on cold start — had any automated coverage before this directory
existed. `docs/WALKTHROUGH.md` §7 lists that gap as items 5, 6 and 8 of what to do next.

- [`TEST-PLAN.md`](TEST-PLAN.md) — the catalogue, the traceability matrix and the known-defect
  watchlist.
- [`journeys/`](journeys) — one XML file per scenario.

## What a journey is

A journey is a test case written as a list of natural-language `<action>` steps. There is no journey
runner binary: an agent evaluates the file, performing each step against the device with
`android layout` and `adb shell input`, and reports per-action JSON at the end. The format and the
reporting contract are defined by the `android-cli` skill
(`~/.claude/skills/android-cli/references/journeys.md`).

**The journey is the source of truth. If the app disagrees with the journey, the app has failed.**
Steps are executed exactly as written and independently of each other; a step that cannot be
performed is a failure, not a prompt to improvise.

### Two local conventions

1. **Setup is not an action.** The journey contract treats a step that is not a UI interaction as a
   malformed journey, so every `adb` precondition lives in `<description>`, verbatim and
   copy-pasteable. Read the description first, run what it says, then start on `<actions>`.

2. **Device-level steps are prefixed "Using adb,".** Rotation, process kill and cold restart are
   device interactions rather than screen interactions, and a handful of scenarios genuinely cannot
   be expressed without them. They are marked so the evaluator performs them rather than rejecting
   the file. There are seven such steps in the whole suite.

## Prerequisites

- A device or emulator on API 26 or higher (`minSdk` is 26), with the **debug** build installed:

  ```bash
  ./gradlew :app:installDebug
  ```

  The release build will not work: the failure-rate hook the suite depends on exists only in the
  debug source set.

- `adb` on the PATH, and the `android` CLI (`android --version`).
- Animations off. Compose transitions otherwise race the layout dump:

  ```bash
  adb shell settings put global window_animation_scale 0
  adb shell settings put global transition_animation_scale 0
  adb shell settings put global animator_duration_scale 0
  ```

If more than one device is attached, add `-s <serial>` to every `adb` command and
`--device <serial>` to every `android` command.

## The mock-source controls

`FakeTaskApi` answers in a uniform 300–800 ms and fails about 15% of calls, by design — that is what
makes loading and error states real states rather than simulations (`REQUIREMENTS.md` §8). It also
makes a scripted journey unrunnable: a scenario that touches the source four times loses a coin flip
roughly half the time, and would fail on the dice rather than on the app.

So the debug build exposes the two knobs `FakeTaskApi` already had internally:

| Knob | Extra | Effect |
|---|---|---|
| `failureRate` | `qa_failure_rate` | `0.0` never fails, `1.0` always fails, anything in between is a probability |
| `latencyMillis` | `qa_latency_ms` | pins every call to exactly this many milliseconds |

Both are debug-only. `app/src/debug/.../qa/QaMockControls.kt` holds the implementation;
`app/src/release/.../qa/QaMockControls.kt` is the same object with an empty body, so the hook is
physically absent from a release build rather than merely switched off in one.

### At launch

Extras on `am start` are applied in `MainActivity.onCreate` before the first composition, which is
what puts them ahead of the list's initial refresh:

```bash
# the standard opening of almost every journey: clean store, honest source
adb shell am force-stop com.rounds.test.to_dolist
adb shell am start -n com.rounds.test.to_dolist/.MainActivity --es qa_failure_rate 0.0

# start on the error screen
adb shell am start -n com.rounds.test.to_dolist/.MainActivity --es qa_failure_rate 1.0

# make the loading state outlast a UI dump (30 s suits `android layout`; 8000 is
# enough for a raw uiautomator dump - see "Reading the device" below)
adb shell am start -n com.rounds.test.to_dolist/.MainActivity \
  --es qa_failure_rate 0.0 --es qa_latency_ms 30000
```

### While the app is running

Some scenarios have to change the source's behaviour *mid-session* — a failure arriving over content
that is already on screen cannot be set up any other way. That is a broadcast:

```bash
adb shell am broadcast -n com.rounds.test.to_dolist/.qa.QaControlReceiver \
  -a com.rounds.test.to_dolist.QA_SET_FAILURE_RATE --es qa_failure_rate 1.0
```

**The `-n` component is not optional.** Android 8 stopped delivering implicit broadcasts to
manifest-declared receivers, so without it the broadcast is accepted, reports `result=0`, and does
nothing at all. Confirm it landed:

```bash
adb logcat -d -s QaMockControls
# I QaMockControls: failureRate = 1.0
```

## Resetting between journeys

```bash
adb shell am force-stop com.rounds.test.to_dolist
```

That is the whole protocol. The store is a `MutableList` inside `FakeTaskApi`, rebuilt from
`SEED_TASKS` in its `init` block, so killing the process restores exactly the four seed rows and
resets both knobs to their defaults. The app writes nothing to disk, so `pm clear` buys nothing.

**Run journeys one at a time, force-stopping between them.** Each one assumes the four seed rows and
nothing else.

## The seed data every journey is written against

| # | Title | Notes | Priority | Completed | Due |
|---|---|---|---|---|---|
| 1 | Renew domain registration | Expires end of month | High | no | in 3 days |
| 2 | Reply to design feedback | *(none)* | Medium | no | — |
| 3 | Book dentist | *(none)* | Low | **yes** | — |
| 4 | Migrate the analytics pipeline to the new warehouse and validate dashboards | Long one — check layout | Medium | no | **yesterday** |

Reproduced from `REQUIREMENTS.md` §8 and implemented in `data/tasks/.../api/SeedData.kt`. Due dates
are stored as day offsets rather than fixed dates so they never go stale, which is why row 1 always
reads "Due in 3 days" and row 4 always reads "Due yesterday". Row 4's title is deliberately long and
must not be shortened — it exists to stress the row layout.

## Reading the device

`android layout -p` returns the screen as a flat JSON list. This is the primary tool; a screenshot
is the fallback for anything about colour.

```bash
android layout -p                                    # whole screen, pretty-printed
android layout -p | grep -E '"(text|content-desc)"'  # just the selectors
android screen capture -o /tmp/screen.png            # when the question is visual
```

**It is slow.** Measured on a Samsung SM-G973F, one `android layout` call takes about 20 seconds on
a settled screen. That does not matter for most assertions, but it rules the tool out for anything
timing-sensitive, and it is why ST-01 pins the source's latency to 30 s rather than to something
that merely feels slow.

The raw dump is roughly seven times faster and carries strictly more information, including the
state attributes `android layout` drops:

```bash
adb exec-out uiautomator dump /dev/tty          # ~3 s, full node attributes
```

Prefer it whenever a step turns on `enabled`, `checked` or the contents of a text field. It is also
what makes ST-01 practical at a lower latency: 8000 is enough if you are reading the screen this
way.

### Where state actually lives in the tree

Compose renders one control as several nodes, and the interesting attributes are not on the node
carrying the text. All three of these were confirmed on the device:

- **Disabled** sits on the clickable parent, not on the label. The Save button's `TextView` reads
  `enabled="true"` even when Save is greyed out; the `android.view.View` wrapping it is the node
  that reads `enabled="false"`. Match the clickable ancestor by bounds.
- **A selected priority chip reports `checked="true"`**, not `selected="true"`. `FilterChip` maps
  onto a checkable node; `selected` is always false and means nothing here.
- **A text field's contents are on the `EditText` node**, which has an empty `content-desc`; its
  label ("Title", "Notes (optional)") is a separate sibling node. Do not expect the label and the
  value on one node.

### Selector traps

There is **no `testTag` anywhere in this codebase** and `testTagsAsResourceId` is off, so every
selector is a visible string or a content description. Four things to know:

- **The key is `content-desc`**, not `contentDesc` as the skill's reference document says.
- **Priority is never row text.** `PriorityIndicator` wraps its label in `clearAndSetSemantics`
  (`core/ui/.../PriorityIndicator.kt:41`), so "High" exists only as a `content-desc`. Searching the
  dump's `text` fields for it will always come up empty.
- **`Loading` is a content description too** (`core/ui/.../StateViews.kt:34`) — the only stable
  handle on the spinner.
- **The no-matches message uses typographic quotation marks**, U+201C and U+201D:
  `No tasks match “zzz”.` A straight-quote comparison fails.

All expected strings come from three resource files and should be quoted from them rather than
retyped: `feature/task-list/src/main/res/values/strings.xml`,
`feature/task-editor/src/main/res/values/strings.xml`, `core/ui/src/main/res/values/strings.xml`.

## Device recipes the scenarios call for

```bash
# rotation — disable auto-rotate first, or the device fights you
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1   # landscape
adb shell settings put system user_rotation 0   # portrait

# process death — am kill is a no-op on a foreground process, so background it first
adb shell input keyevent KEYCODE_HOME
adb shell am kill com.rounds.test.to_dolist
# alternative: Developer options → "Don't keep activities"

# dark mode
adb shell cmd uimode night yes
adb shell cmd uimode night no

# font scale
adb shell settings put system font_scale 2.0
adb shell settings put system font_scale 1.0
```

Time zone is the one recipe with no reliable adb form: `service call alarm 3 s16 <zone>` needs
`SET_TIME_ZONE` and is OEM-dependent. Only DD-04's absolute-date fallback is zone-sensitive, and it
does not need the zone changed — just a device whose clock is correct.

## Reporting a run

Report per-action JSON exactly as the journey contract specifies — the action's full text, a status
of `PASSED`, `FAILED` or `SKIPPED`, the adb commands used, and a comment:

```json
{
  "journey": "ST-04 A failure over a loaded list does not blank it",
  "results": [
    { "action": "Verify that \"Renew domain registration\" is on screen",
      "status": "PASSED", "commands": ["android layout -p"] }
  ]
}
```

Evaluation stops at the first failed action; everything after it is `SKIPPED`. Keep debugging to a
minimum during a run — the job is to find out whether the app passes, not to fix it. Five journeys
are *expected* to fail; check them against the watchlist in `TEST-PLAN.md` before filing anything.
