# Task 1 – progress

Deliverables from `../aufgabe-1.pdf` (pages 6–7) and the steps agreed in the sessions. Tick a box when the step is done and committed.

## Part A – Specification
- [x] One-page spec, marked final (`docs/specification.md`)

## Part B – Implementation with an AI assistant
- [x] Project skeleton; build and emulator pipeline working
- [x] App generated from the spec by a sub-agent; prompt and report saved (`docs/generation-prompt.md`, `docs/generation-report.md`)
- [x] Smoke test on the emulator with screenshots (`docs/screenshots/`)

## Part C – Code review (`docs/review.md`)
- [x] Review against the PDF checklist and `docs/guidelines.md`, including the 15 assumptions in `docs/generation-report.md`
- [x] Check on the emulator what the smoke test didn't cover: GPS permission flow, rate switch, Reset, pause in the background, missing sensor
- [x] At least 3 findings, each with location, problem, impact and fix
- [x] Points the AI got right, with how we know
- [x] Fixes, one commit each (F1–F5)

## Part D – Verification by measurement (`docs/measurements.md`)
- [x] Accelerometer experiment: setup, expected value with source, mean of several runs, deviation, explanation
- [x] Barometer experiment, same structure
- [x] GPS experiment, same structure, including the jitter filter
- [x] Event rate at `SENSOR_DELAY_NORMAL` vs. `SENSOR_DELAY_GAME`, compared with the requested rate; consequence for energy use

## Part E – Assessment (`docs/assessment.md`)
- [ ] Edge vs. cloud for activity recognition: latency, energy, offline capability, privacy
- [ ] Which sensor data is personal or allows conclusions about a person
- [ ] Citable sources, e.g. for model size and on-device recognition

## AI log (`docs/ai-log.md`, max. one page)
- [ ] Table from the template on page 4 of the PDF
- [ ] Answers: where the AI saved time; where it made errors and how they were noticed (source: `docs/ai-errors.md`)
- [ ] Inputs named: spec with process wrapper, CLAUDE.md files, guidelines

## Environment info
- [ ] Android 17 / API 37, emulator "Medium Phone API 37.0", Claude Code with Claude Opus 5.5

## Submission
- [ ] Condense `review.md` and `measurements.md` for the PDF, always from the full docs (trim list in `HANDOFF.md`, next step 5)
- [ ] PDF built with pandoc + WeasyPrint; stylesheet in the repo
- [ ] Title page with name and matriculation number (filled in by the student)
- [ ] File name `aufgabe_1_<surname>.pdf`
- [ ] Code: ZIP of `src` without build folders, or link to the public GitHub repo
- [ ] Upload to Ilias

## Housekeeping
- [x] Optional: remove the outdated "Candidates for the spec" section from `docs/guidelines.md`
