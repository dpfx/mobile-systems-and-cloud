# Task 2 – progress

Deliverables from `../aufgabe-2.pdf` (pages 8–9) and the general rules (pages 4–5). Tick a box when the step is done and committed.

## Open questions (for the lecturer)
- [ ] Where are the vibe-coded prototype and the `firebase_example`?
- [ ] Which due date applies: 24.11.2026 (schedule, page 5) or 30.11.2026 (task text, page 9)?
- [ ] Part A.1: minimum number of findings (missing in the PDF: "mindestens … Befunden aus den (Sicherheit, …)")
- [ ] Optional: is a backend stack other than the lab's (e.g. Laravel) fine?

## Setup
- [ ] Prototype and `firebase_example` in the repo
- [ ] Prototype running locally
- [ ] Claude API: separate workspace with its own key and a spend limit on platform.claude.com, if the org allows it (student)
- [ ] Model chosen (student); prices at the time of writing: Claude Haiku 5.5 $0.10 / $0.50, Claude Opus 5.5 $4 / $20 per million input / output tokens

## Part A – Review of the prototype (`docs/findings.md`)
- [ ] A.1 Findings list **without AI** (student): security, architecture and cloud operation, AI-specific risks, privacy and law, quality; per finding: location, problem, impact, severity (high/medium/low), measure
- [ ] A.2 Two findings proven on the locally running prototype (e.g. curl, browser dev tools)
- [ ] A.3 AI review of the same code (raw output saved), compared: what the AI found in addition, what it missed, which of its findings are wrong or exaggerated

## Part B – Architecture decision (`docs/architecture.md`)
- [ ] V1 (app calls the AI directly), V2 (own backend), V3 (BaaS with managed AI) compared on: API key protection, cost control, privacy (place of processing), vendor lock-in, latency, development effort, scalability
- [ ] Decision with justification
- [ ] Architecture drawing (draw.io or ArchiMate): components, data flows, trust boundaries, region of data storage

## Part C – Implementation
- [ ] Authentication; each person sees only their own reports
- [ ] No API key of the AI service in the app
- [ ] Structured AI answer (JSON), validated on the server (allowed values, lengths); defined behaviour on invalid answer, timeout and outage of the AI service
- [ ] Storage in a cloud database
- [ ] Cost protection: limited input length, rate limit per person, limited answer length
- [ ] Versioning with Git
- [ ] Findings from Part A fixed, as far as they concern the architecture

## Part D – Quality of the AI answers (`docs/evaluation.md`)
- [ ] Test set: ≥ 10 fault descriptions with expected priority, including ≥ 3 edge cases (e.g. unintelligible, very long, other language) and 2 prompt-injection attempts
- [ ] Each case run three times; documented: share of valid JSON answers, agreement with the expected priority, consistency across runs, behaviour on the edge cases
- [ ] Which weaknesses of the model the validation catches, which not
- [ ] Evaluation automated with a script (reused in Task 3)

## Part E – Operation, costs and law (`docs/operations.md`)
- [ ] Token usage of one request (`usage` field) and monthly cost estimate (e.g. 50 users × 20 requests per day) from the provider's current price list
- [ ] Cost risk of the key in the app (as in the prototype)
- [ ] Privacy: which personal data reaches the AI provider, where it is processed, which measures
- [ ] EU AI Act: risk class and resulting obligations

## AI log (`docs/ai-log.md`, max. one page)
- [ ] Template table (page 4 of the PDF) and answers: where the AI saved time; where it made errors and how they were noticed (source: `docs/ai-errors.md`)

## Submission
- [ ] Source code (client and backend, without keys) as ZIP or link to the Git repo
- [ ] PDF: findings list, evidence and comparison with the AI review, architecture comparison and drawing, screenshots (app: entry form, AI suggestion, list; cloud console: database, authentication), evaluation, Part E, AI log
- [ ] Environment info (platform, cloud services, model)
- [ ] Title page with name and matriculation number; file name `aufgabe_2_<surname>.pdf`
- [ ] Upload to Ilias
