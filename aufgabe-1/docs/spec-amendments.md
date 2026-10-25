# Spec amendments – Task 1

`specification.md` is the one-page spec that served as the prompt for generating the app (Part A). It is frozen and no longer edited. Changes found necessary later are recorded here as numbered amendments. **The current requirements are `specification.md` plus these amendments; where they differ, the amendment applies.**

## A1 · 2026-10-25 · Section 7 (Architecture) · Add kotlinx-coroutines to the allowed libraries

| | |
|---|---|
| **Original** | "Only AndroidX, Compose (BOM, Material 3), Vico, all pinned." |
| **Amended** | "Only AndroidX, Compose (BOM, Material 3), Vico, kotlinx-coroutines, all pinned." |
| **Reason** | The same section requires `callbackFlow` and `StateFlow`, which come from `kotlinx-coroutines`, but its library list left that library out. The generated code therefore used it without declaring or pinning it. |
| **Finding** | F2 (`review.md`, item 7) |
