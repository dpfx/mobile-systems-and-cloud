# AI errors – Task 1

Chronological record of every error the AI (Claude Code, Claude Opus 5.5) made in this task, with how it was noticed and how it was corrected. Times are local German time: CEST (UTC+2) until 2026-10-25 03:00, CET (UTC+1) afterwards. This is the raw material for the one-page AI log (`ai-log.md`), in particular its question "Where did the AI make errors, and how did you notice them?".

Categories: **Build** (build configuration, dependencies), **Code** (app code), **Knowledge** (outdated or wrong facts), **Reasoning** (flawed argument or justification), **Tooling** (commands and scripts the AI runs), **Workflow** (acting against how the user wants to work).

## 1 · 2026-10-24 16:11 · Workflow · Committed without the user reviewing

- **What the AI did:** created the initial Git commit itself (`9af460b`) right after setting up the repository.
- **How it was noticed:** the user saw the commit and asked for a different workflow.
- **Impact:** none on content; the user didn't get to review the commit before it was made.
- **Correction:** new rule in `CLAUDE.md` › Git workflow: Claude only stages changes and drafts the commit message; the user reviews and commits in VS Code.

## 2 · 2026-10-24 16:24 · Build / Knowledge · Compiled against API 36 although the libraries need API 37

- **What the AI did:** set `compileSdk = 36` and `targetSdk = 36` in the project skeleton, assuming API 36 was the safe stable choice, without checking what the current Compose libraries (BOM `2026.09.00`, Compose 1.12.1) require.
- **How it was noticed:** the first build (`./gradlew assembleDebug`) failed with nine errors of the form *"Dependency 'androidx.compose.ui:ui-android:1.12.1' requires libraries and applications that depend on it to compile against version 37 or later of the Android APIs."*
- **Impact:** the build was broken; nothing was installed.
- **Correction:** `compileSdk = 37` (notation from the [Android 17 SDK setup page](https://developer.android.com/about/versions/17/setup-sdk)), `targetSdk = 37` because the app is tested on an Android 17 emulator, and SDK platform `android-37.0` installed. The build then succeeded and the app ran on the emulator.
- **Evidence:** diff of `aufgabe-1/app/build.gradle.kts` between the raw skeleton commit and the fix commit.

## 3 · 2026-10-24 16:48 · Workflow · Asked for five design decisions at once

- **What the AI did:** while preparing the spec, presented five design decisions (location source, chart, warning logic, background behaviour, rate switch) mixed into one long message about all six spec points, and asked for all answers at once.
- **How it was noticed:** the user (16:52) found the decisions too scattered and unstructured to talk through, which made planning uncomfortable.
- **Impact:** none on content; the decisions were then made one by one.
- **Correction:** new rule in `CLAUDE.md` › Working with Claude: first a short overview list (one line per decision), then each decision on its own in a fixed format (context, options table, recommendation), waiting for the answer before the next one.

## 4 · 2026-10-24 17:02 · Workflow · Improvised a workaround instead of asking for a missing tool

- **What the AI did:** to check whether the spec fits on one page, it needed a PDF engine for `pandoc`. None was installed in WSL, so instead of asking, it printed the spec to PDF with headless Microsoft Edge on the Windows side.
- **How it was noticed:** the user (17:03) pointed out that the AI should have asked whether a standard tool could be installed.
- **Impact:** the page count was measured, but with an ad-hoc setup that's hard to reproduce, and the tooling decision was taken away from the user.
- **Correction:** new rule in `CLAUDE.md` › Working with Claude: if a standard tool is missing, name it and ask the user to install it; no improvised workarounds. The user then installed WeasyPrint as the PDF engine for `pandoc`.

## 5 · 2026-10-24 17:36 · Reasoning · Justified the fall notification with an unrealistic scenario

- **What the AI did:** asked why a notification is needed when the app does nothing in the background, it argued that after a fall the phone "may be on the ground or in a pocket" where nobody sees the in-app banner.
- **How it was noticed:** the user (17:37) pointed out that the app only measures while it is on screen, so the phone is unlocked and in the hand, not in a pocket; a drop means the phone slipped out of the hand.
- **Impact:** a weak argument nearly went unchallenged into the design; it also hid that the 25 m/s² rule detects an impact on the *phone* (e.g. a dropped phone), not a fall of the *person*.
- **Correction:** the notification is kept because the task requires it (Part A "Benachrichtigung", Part C notification permission); the warning was renamed to "Strong shock detected" (switch: "Shock alerts") so it claims only what the sensor can tell.

## 6 · 2026-10-25 12:41 · Tooling · Created and staged a doc with an empty prompt

- **What the AI did:** built `docs/generation-prompt.md` with a shell script that copied the prompt from a temporary file. The temporary file had been deleted overnight; the script didn't stop on the error, so it created the file with an empty prompt block and staged it.
- **How it was noticed:** the AI read the script's output (`No such file or directory`) before presenting the result.
- **Impact:** none; nothing was committed.
- **Correction:** the prompt was taken from the Claude Code session log instead (exactly what the sub-agent received), checked to be identical to the committed spec, and the file was rebuilt with a script that stops on any error.

## 7 · 2026-10-25 13:34 · Tooling · Drove the emulator without checking that it was running

- **What the AI did:** started the multi-step lifecycle check (Part C) without first checking `adb devices`. The emulator had been shut down overnight, so the first `adb` call waited for a device and the command hung until it was moved to the background.
- **How it was noticed:** the command timed out; its output showed "waiting for device".
- **Impact:** none; the waiting command was stopped before the emulator came back, so no step ran unobserved.
- **Correction:** emulator checks start with `adb devices` and stop if no device is listed.

## 8 · 2026-10-25 13:50 · Tooling · Emulator start command blocked on its own output pipe

- **What the AI did:** started the emulator from WSL with `cmd.exe /c start … | tr … | grep …`. The emulator inherited the pipe, so the pipeline could only end when the emulator exits; the following boot check never ran and the command hung until its timeout.
- **How it was noticed:** the command timed out with empty output, although the emulator had started.
- **Impact:** about 9 minutes lost; the emulator itself was fine.
- **Correction:** check the emulator with separate, short `adb` calls wrapped in `timeout`; never pipe the output of a process that keeps running.

## 9 · 2026-10-25 14:23 · Tooling · Converted log times with a fixed UTC+2 offset across the DST change

- **What the AI did:** took the times for entries 6 and 8 from the session log (UTC) and converted them with a fixed offset of +2 h. Daylight saving time had ended at 03:00 that night, so both times were one hour too late, and entry 6 appeared after entry 7.
- **How it was noticed:** while checking emulator timestamps (emulator in UTC, host in CET), the AI compared the clocks and re-read the log.
- **Impact:** two wrong times in this file; nothing else.
- **Correction:** entries 6 and 8 corrected to CET; the header now states which time zone applies when; conversions use the `Europe/Berlin` time zone instead of a fixed offset.

## 10 · 2026-10-25 14:25 · Tooling · Corrupted emulator screenshots with a text filter

- **What the AI did:** wrapped all `adb` calls in a helper that pipes output through `tr -d '\r'` (to remove Windows line endings) and also used it for `screencap`, which deleted bytes from the binary PNG files.
- **How it was noticed:** the image could not be opened for checking.
- **Impact:** two review screenshots in the scratchpad were unusable; one was retaken, the other wasn't needed.
- **Correction:** binary output (screenshots) is written without any filter; the helper is only used for text.

## 11 · 2026-10-25 14:42 · Tooling · Test commands stacked several copies of the app's screen

- **What the AI did:** during the Part C emulator checks, it "returned" to the app with `am start -n …/.MainActivity`. Without the launcher's action and category, this creates a new `MainActivity` on top of the existing one each time, and each instance has its own ViewModel. In addition, its button finder added a trailing space to labels, so `^Grant$` never matched and a Back key press hit the wrong screen.
- **How it was noticed:** the activity in the foreground had a different ID than the one Back returned to; `dumpsys activity` then listed four `MainActivity` instances.
- **Impact:** the permission test had to be repeated. In the lifecycle check (review item 1), "back to the app" opened a new instance instead of resuming; the core result (listeners only active while visible) still holds, but that step is repeated with the proper launch.
- **Correction:** the app is now started like the launcher does it (`-a MAIN -c LAUNCHER`, which brings the existing task to the front), the instance count is checked, and labels are trimmed before matching.

## 12 · 2026-10-25 14:50 · Tooling · Ran a test step from a state that couldn't reach it

- **What the AI did:** in the permission check (Part C), it tried to test "approximate location only" right after the permanent-denial test. The app's ViewModel still remembered the denial, so the card showed "Grant" instead of "Enable GPS" and the step was skipped; the next step then ran with a leftover GPS position.
- **How it was noticed:** the script reported "NOT FOUND: Enable GPS".
- **Impact:** the step had to be repeated with a fresh app process.
- **Correction:** each test step that needs a clean state now starts from one (permission revoked, which also ends the app process), and the expected starting screen is checked first.

## 13 · 2026-10-25 15:23 · Code · Skeleton without an app icon, although described as template-equivalent

- **What the AI did:** created the project skeleton (Part B, 2026-10-24) and described it as equivalent to Android Studio's "Empty Activity" template, but left out the launcher icon that the template includes.
- **How it was noticed:** Android Lint in the code review (Part C, item 7): `MissingApplicationIcon` – "Should explicitly set android:icon, there is no default".
- **Impact:** the app shows Android's generic placeholder icon on the home screen and in the notification shade.
- **Correction:** fixed as review finding F5: an adaptive launcher icon (vector, Android 8.0+) was added; Lint no longer reports `MissingApplicationIcon`.

## 14 · 2026-10-25 16:20 · Tooling · Measurement script's checks failed because of pipefail and grep -q

- **What the AI did:** wrote `tools/measure.sh` with `set -o pipefail` and checks of the form `adb … | grep -q …`. `grep -q` stops reading at the first match, the `adb` command then fails with a broken pipe, and `pipefail` turns that into a failed check, exactly when the match was found. The AI first suspected a sleeping emulator screen.
- **How it was noticed:** the first run stopped during setup with "Sensor Lab is not in front", although the device state showed the app in front and the screen awake.
- **Impact:** no data; the run stopped after 3 seconds.
- **Correction:** the checks capture the output in a variable first and then test it, so no pipe is cut short; the setup also wakes the screen before the first check.

## 15 · 2026-10-25 17:00 · Tooling · Measurement script walked the first GPS step too fast

- **What the AI did:** in `tools/measure.sh`, the GPS walk started 2 s after Reset, while all other steps were 4 s apart. The first fix after Reset (the start point) sometimes arrived up to 1.5 s late, so the first step of 11 m came only 1 s after it.
- **How it was noticed:** two of five runs measured 100.3 m instead of 111.2 m. A live recording of the app's log showed `rejected: step 10.9 m in 1.0 s = 10.9 m/s > 10 m/s`: the app correctly applied the plausibility limit (amendment A3) to the test's own too-fast step.
- **Impact:** the first GPS results were unusable as a measurement of the walk; the jitter and jump results were not affected.
- **Correction:** the script now waits 6 s after Reset, so the first step is as slow as the others; the GPS section was measured again.

## 16 · 2026-10-25 17:25 · Reasoning · Explained the GPS step sizes with a hypothesis its own data contradicts

- **What the AI did:** in `HANDOFF.md`, explained why single GPS steps alternate between 10.9 m and 11.3 m with a position grid of ~0.42 m, "the resolution of a 32-bit float at 48°". It marked the mechanism as not confirmed, but didn't check the hypothesis against the measured data.
- **How it was noticed:** while preparing `measurements.md`, the AI computed what 32-bit float rounding of the walk's positions would produce: steps of 11.03 m and 11.45 m (not alternating) and a total of 111.1 m, while the app measured 111.2 m in all runs.
- **Impact:** none on the results; the wrong explanation would have gone into the measurement report.
- **Correction:** hypothesis dropped. `measurements.md` reports the observed step sizes with a new log excerpt (`measurements/gps-steps.log`) and states that the mechanism was not determined; `HANDOFF.md` was corrected.

## 17 · 2026-10-25 20:32 · Reasoning · Proposed research beyond what the task asks

- **What the AI did:** in Part E, it proposed backing "recognition on the device is feasible" with a scientific source and a model size, and spent several searches on a paper (Anguita et al. 2012) whose abstract it couldn't retrieve. The idea came from a `TODO.md` item the AI itself had written in an earlier session ("e.g. for model size"), not from the task PDF.
- **How it was noticed:** the user asked "do we really have to go into that detail? what does the task description say?". The PDF only asks for a justified choice along four criteria and which data is personal.
- **Impact:** time spent on searches; nothing went into the docs.
- **Correction:** the feasibility source was dropped and the `TODO.md` item reworded to "citable sources for the claims made"; research is now checked against the PDF's wording before it is proposed.
