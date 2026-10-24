# AI errors – Task 1

Chronological record of every error the AI (Claude Code, Claude Opus 5.5) made in this task, with how it was noticed and how it was corrected. Times are CEST. This is the raw material for the one-page AI log (`ai-log.md`), in particular its question "Where did the AI make errors, and how did you notice them?".

Categories: **Build** (build configuration, dependencies), **Code** (app code), **Knowledge** (outdated or wrong facts), **Workflow** (acting against how the user wants to work).

## 1 · 2026-10-24 16:11 · Workflow · Committed without the user reviewing

- **What the AI did:** created the initial Git commit itself (`47e6480`) right after setting up the repository.
- **How it was noticed:** the user saw the commit and asked for a different workflow.
- **Impact:** none on content; the user didn't get to review the commit before it was made.
- **Correction:** new rule in `CLAUDE.md` › Git workflow: Claude only stages changes and drafts the commit message; the user reviews and commits in VS Code.

## 2 · 2026-10-24 16:24 · Build / Knowledge · Compiled against API 36 although the libraries need API 37

- **What the AI did:** set `compileSdk = 36` and `targetSdk = 36` in the project skeleton, assuming API 36 was the safe stable choice, without checking what the current Compose libraries (BOM `2026.09.00`, Compose 1.12.1) require.
- **How it was noticed:** the first build (`./gradlew assembleDebug`) failed with nine errors of the form *"Dependency 'androidx.compose.ui:ui-android:1.12.1' requires libraries and applications that depend on it to compile against version 37 or later of the Android APIs."*
- **Impact:** the build was broken; nothing was installed.
- **Correction:** `compileSdk = 37` (notation from the [Android 17 SDK setup page](https://developer.android.com/about/versions/17/setup-sdk)), `targetSdk = 37` because the app is tested on an Android 17 emulator, and SDK platform `android-37.0` installed. The build then succeeded and the app ran on the emulator.
- **Evidence:** diff of `aufgabe-1/app/build.gradle.kts` between the raw skeleton commit and the fix commit.
