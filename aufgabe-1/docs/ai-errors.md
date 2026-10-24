# AI errors – Task 1

Chronological record of every error the AI (Claude Code, Claude Opus 5.5) made in this task, with how it was noticed and how it was corrected. Times are CEST. This is the raw material for the one-page AI log (`ai-log.md`), in particular its question "Where did the AI make errors, and how did you notice them?".

Categories: **Build** (build configuration, dependencies), **Code** (app code), **Knowledge** (outdated or wrong facts), **Reasoning** (flawed argument or justification), **Workflow** (acting against how the user wants to work).

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
