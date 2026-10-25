# Aufgabe 1 – Sensorik

Task description: `../aufgabe-1.pdf`, pages 6–7. Deadline: 2026-10-26 (upload on Ilias).

Progress is tracked in `TODO.md`; update it when a step is done.

## The spec is the source of truth
- What the app does is defined **only** by `docs/specification.md` (Part A; it is also the prompt that generates the app).
- Don't add features, sensors, libraries or behaviour that the spec doesn't require. If the spec is unclear or incomplete, ask instead of guessing, and record the gap so the spec can be improved.
- The spec was marked final on 2026-10-24. Any later change to it must be recorded with what changed and why, because the PDF requires the spec to be the prompt.
- This file is also input to the AI, so list it in the AI log.

## Documentation (`docs/`, English)
| File | PDF section |
|---|---|
| `specification.md` | Part A – spec (= prompt), max. 1 page |
| `guidelines.md` | Pinned best practices with sources; the key rules are in the spec, the full list was given to the generating AI as coding rules and is the Part C review checklist |
| `review.md` | Part C – ≥ 3 findings: location, problem, impact, fix (+ Git commit) |
| `measurements.md` | Part D – per sensor: setup, expected value + derivation/source, measured mean, deviation, explanation; event rate at two delay settings |
| `assessment.md` | Part E – edge vs. cloud; personal data |
| `ai-errors.md` | Chronological record of every AI error (date/time, what happened, how it was noticed, correction). **Add an entry as soon as an AI error is found.** Raw material for `ai-log.md` |
| `ai-log.md` | AI log (template on page 4 of the PDF) |
| `screenshots/` | display modes, a triggered warning, logging chain |

## Verification on the emulator
- No physical device: Part D experiments inject known values (`adb emu sensor set …`, `adb emu geo fix …`) and compare them with what the app displays and logs. State this in `measurements.md`.
