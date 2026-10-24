# Praktikum Mobile Systeme und Cloud (WS 2026/27)

University lab course, HS Albstadt-Sigmaringen. Done solo, although the PDF assumes groups of two. The assignment PDFs are in the repo root (`aufgabe-1.pdf`, …). Each task has its own folder (`aufgabe-1/`, …) with its own `CLAUDE.md`. **Read that file before working on the task.**

## Course rules (apply to every task)
- The student must be able to explain every line of the submission, so prefer plain, readable code over clever code.
- No secrets in code or Git: API keys go in environment variables or `.env`, which is gitignored. Never put third-party personal data in prompts.
- Use free tiers only.
- **Everything is in English**: code, comments, commit messages, docs and the submission PDF. Docs follow the course rule "as little as possible, as much as necessary": no background chapters; every claim backed by a screenshot, measurement or excerpt.
- Every submission includes an AI log (max. one page, template on page 4 of the PDF). Keep AI work traceable for it (see Git workflow).

## Git workflow
- Commit raw AI-generated code **unchanged** first ("raw AI output: …"), then commit each review fix separately, naming the finding it fixes. The diffs are the evidence for the code review.
- Never commit `build/`, `.gradle/`, `local.properties`, `.env` or IDE files.

## Environment
- Code and builds run in WSL (Gradle, JDK 21, Android SDK at `~/Android/Sdk` via `local.properties`).
- The Android emulator runs on the Windows host. Control it from WSL with the **Windows** `adb.exe` from the Windows Android SDK; Linux `adb` can't reach it.
