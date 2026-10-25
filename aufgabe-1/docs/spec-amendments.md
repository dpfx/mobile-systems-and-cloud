# Spec amendments – Task 1

`specification.md` is the one-page spec that served as the prompt for generating the app (Part A). It is frozen and no longer edited. Changes found necessary later are recorded here as numbered amendments. **The current requirements are `specification.md` plus these amendments; where they differ, the amendment applies.**

## A1 · 2026-10-25 · Section 7 (Architecture) · Add kotlinx-coroutines to the allowed libraries

| | |
|---|---|
| **Original** | "Only AndroidX, Compose (BOM, Material 3), Vico, all pinned." |
| **Amended** | "Only AndroidX, Compose (BOM, Material 3), Vico, kotlinx-coroutines, all pinned." |
| **Reason** | The same section requires `callbackFlow` and `StateFlow`, which come from `kotlinx-coroutines`, but its library list left that library out. The generated code therefore used it without declaring or pinning it. |
| **Finding** | F2 (`review.md`, item 7) |

## A2 · 2026-10-25 · Section 3 (Warning rule) · Define what tapping the notification does

| | |
|---|---|
| **Original** | "…and, if the **Shock alerts** switch is on, a notification; at most once per 30 s." |
| **Amended** | "…and, if the **Shock alerts** switch is on, a notification (tapping it opens the app); at most once per 30 s." |
| **Reason** | Android requires every notification to respond to a tap ("Every notification must respond to a tap, usually to open an activity in your app", [Build a notification › Set the notification's tap action](https://developer.android.com/develop/ui/views/notifications/build-notification#tap)). The generated code added this behaviour on its own, but opened a second screen with fresh data. |
| **Finding** | F4 (`review.md`, section 9) |
