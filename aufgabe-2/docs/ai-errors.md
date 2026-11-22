# AI errors – Task 2

Chronological record of the errors the AI (Claude Code, Claude Opus 5.5) made in this task, with how they were noticed and how they were corrected. Listed are errors that produced a wrong result or forced a step to be redone; command slips fixed in the next attempt are not listed. Times are local German time (CET, UTC+1). This is the raw material for the one-page AI log (`ai-log.md`), in particular its question "Where did the AI make errors, and how did you notice them?".

Categories: **Build** (build configuration, dependencies), **Code** (app code), **Knowledge** (outdated or wrong facts), **Reasoning** (flawed argument or justification), **Tooling** (commands and scripts the AI runs), **Workflow** (acting against how the user wants to work).

## 1 · 2026-11-22 13:00 · Workflow · Wrote internal account details into a committed file

- **What the AI did:** when recording that the user's own Claude API account may be used for the course, it also wrote organisational details about that account (approval and monthly budget) into the root `CLAUDE.md` and into the commit message it drafted. They contained no API key, password or other credential and no personal data of third parties, but the user wants such details kept out of the repository. The AI had also prepared the session handoff, which contained the same details, for committing.
- **How it was noticed:** the user pointed out that the handoff shouldn't contain such details and shouldn't be committed. The AI then checked all committed files and commit messages and found the two places.
- **Impact:** the line was in the repository for about 30 minutes; no credentials were exposed.
- **Correction:** the line was rewritten without those details, and the commit was amended (`657901d`) and force-pushed. Handoffs are now gitignored (`HANDOFF.md`), and Task 1's was untracked. Before staging, the AI checks every diff and commit message for details that don't belong in the repository.
