# Aufgabe 2 – Cloud und KI-Dienst

Task description: `../aufgabe-2.pdf`, pages 8–9 ("Vom Vibe-Coding-Prototyp zur sicheren Cloud-Architektur"). The PDF is the whole course handout (revision of 2026-11-17): pages 1–5 are the general rules for all tasks, pages 6–7 repeat Task 1 unchanged. Due date unclear: 24.11.2026 in the schedule (page 5), 30.11.2026 in the task text (page 9); see the open questions in `TODO.md`.

Progress is tracked in `TODO.md`; update it when a step is done.

## Goal
Connect a cloud LLM securely to a mobile application (browser-based is fine). Starting point: a vibe-coded prototype from the lecturer that works but isn't production-ready. The app domain follows the PDF: fault reports ("Meldungen", "Störungsbeschreibungen") for which the AI suggests a priority. Implementation as a new development or by reworking the prototype. Task 3 builds on the backend from this task.

## Rules for this task
- **Part A.1 is done without AI** (PDF: "Führen Sie das Review zunächst ohne KI durch"). The student reviews the prototype alone. Until the student's findings list is committed, Claude doesn't read, summarise or comment on the prototype's code and gives no hints about its weaknesses. Help with running it locally only if the student asks, without comments on what it shows.
- **Part A.3:** Claude's AI review of the same code is saved unchanged (raw output) before it is compared with the student's list, so the comparison is traceable.
- **Part B before Part C:** the architecture follows from comparing V1, V2 and V3 on the seven criteria of the PDF; the stack isn't fixed before that comparison.
- **Secrets:** API keys only in environment variables, `.env` (gitignored) or the platform's secret store; never in client code, the repo, logs, screenshots or prompts.
- **LLM: the user's Claude API** (the approved exception to free tiers, see `../CLAUDE.md`). The model is the user's choice. Tokens are handled carefully: small `max_tokens`, no unneeded calls, the rate limit per person; no API call by Claude without the user's explicit go. The key lives only in the backend's secret store or `.env`.
- **Costs:** free tiers for everything else (hosting, database, authentication); wherever a credit card is required, set a budget limit or cost alert first (PDF page 4).
- **Personal data:** test data is invented; no real third-party data in prompts or test sets.
- **Task 3 builds on this backend:** keep it deployable, testable and reproducible from the repo.

## Documentation (`docs/`, English)
Same pattern as Task 1: `docs/` is the full record; the PDF is built from condensed versions in `submission/` (reuse `../aufgabe-1/submission/build.sh` and its stylesheet).

| File | PDF section |
|---|---|
| `findings.md` | Part A – findings list (location, problem, impact, severity, measure), two findings proven on the running prototype, comparison with the AI review |
| `architecture.md` | Part B – V1/V2/V3 compared on the seven criteria, decision, architecture drawing (components, data flows, trust boundaries, data region) |
| `implementation.md` | Part C – how each minimum requirement is met, with evidence |
| `evaluation.md` | Part D – test set, results of three runs per case, what the validation catches and what not |
| `operations.md` | Part E – token usage and monthly cost, cost risk of the key in the app, privacy, EU AI Act |
| `environment.md` | Development environment (platform, cloud services, model) |
| `ai-errors.md` | Chronological record of AI errors, same threshold as Task 1. **Add an entry as soon as an AI error is found.** Raw material for `ai-log.md` |
| `ai-log.md` | AI log (template on page 4 of the PDF), max. one page; required by the general rules even though the Task 2 hand-in list doesn't name it |
| `screenshots/` | App: entry form, AI suggestion, list; cloud console: database, authentication |
