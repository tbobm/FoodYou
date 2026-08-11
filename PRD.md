# PRD: personal calorie tracker (Food You fork)

**How to use this file.** Place it at the repository root as `PRD.md`. Reference it from `CLAUDE.md` with a single line: `Read PRD.md before any work. It is the source of truth for scope, data model rules, and gates.` Then start a Claude Code session with: `Read PRD.md and execute Phase 0. Stop at the gate.`

---

## 0. Mission

Replace a paid MyFitnessPal subscription with a self-maintained Android calorie tracker, forked from Food You (`https://github.com/maksimowiczm/FoodYou`, Kotlin/Compose, F-Droid distributed).

Primary user: the repository owner, single device, daily use.
Secondary: up to 20 friends via a Google limited distribution account, only after the app has been the owner's daily driver for one month.

Success is measured by continued daily use, not by feature count. An app that is correct and tedious is a failure.

## 1. Non-negotiable constraints

1. **Local-first.** No backend service. No account system. No telemetry. All data in the on-device database.
2. **Snapshot at write time.** Log entries store resolved nutrition and cost values, not references that are recomputed on read. Editing a food or recipe must never alter historical entries.
3. **Human owns the schema.** Any change to database entities or any Room migration must be proposed in writing and approved before implementation. See Section 2.
4. **Barcodes are EAN-13/UPC.** Not QR codes. Not OCR. Use the existing scanner in the fork.
5. **Licence compliance.** Verify the upstream licence before any redistribution. The upstream maintainer states he does not accept code contributions due to licensing and because the app generates income. Forking may still be permitted by the licence, but confirm the terms and preserve all notices, and publish modified sources if the licence requires it.
6. **Tap budget.** Logging a previously eaten meal must take no more than two taps from app launch. This is an acceptance criterion, not an aspiration.

## 2. Operating rules for the agent

### Gates requiring explicit human approval

Stop and wait for approval before:

- Creating or altering any Room entity, DAO signature, or migration
- Changing the package name or application ID
- Adding a new third-party dependency
- Deleting or rewriting any upstream module rather than extending it
- Any work that touches the MyFitnessPal import once real data has been imported

At each gate, output: the proposed change, the migration SQL if applicable, the rollback path, and what existing data could be affected.

### Verification loop

Every implementation task ends with all of the following, or the task is not complete:

```bash
./gradlew --offline assembleDebug   # must succeed
./gradlew --offline test            # must pass
adb install -r <apk>
adb exec-out screencap -p > /tmp/screen.png
```

Read the screenshot back into the session and confirm the UI matches intent. Do not report a UI task complete based on code inspection alone.

Where the fork already has screenshot or instrumentation tests, extend them rather than adding a parallel harness.

### Working style

- Prefer extending upstream abstractions over introducing new ones. This fork must remain rebaseable against upstream for at least the first year.
- Keep every feature in its own commit with a message explaining why, not what.
- When upstream already solves a problem imperfectly, fix upstream's version rather than writing a second implementation alongside it.
- If a requirement in this document conflicts with the fork's existing architecture, stop and report the conflict rather than forcing it.
- Do not add features not listed here. Log ideas in `IDEAS.md`.

## 3. Phase 0: discovery (no code)

Deliverables, in this order:

1. **`CLAUDE.md`**: module map of the fork. For each module: purpose, key entry points, and whether this project will touch it. Include the navigation graph and where the daily diary screen is composed.
2. **`docs/schema.md`**: the current database schema as it actually exists. Every entity, every relationship, current schema version, and the existing migration chain. Note explicitly whether logged entries currently store snapshotted nutrition values or resolve them from a food reference at read time. This determines the size of Phase 1.
3. **`docs/licence.md`**: the upstream licence, its redistribution obligations, and what must be done before sharing an APK.
4. **`docs/tap-audit.md`**: current tap count for these flows, measured on device: log a barcode-scanned product; log a previously logged food; log a saved recipe; add a manual entry with estimated calories.
5. **`docs/gaps.md`**: for each feature in Section 5, state whether it exists, partially exists, or is absent, with file references.

Then stop. Do not begin Phase 1.

## 4. Data model requirements

These apply to whatever the fork already has. If the fork already satisfies a requirement, say so and change nothing.

### Log entry

Each diary entry must persist, at write time:

- Timestamp (instant plus local date, so that timezone changes do not move entries between days)
- Meal slot (breakfast, lunch, dinner, snack, user-definable)
- Quantity and unit as entered by the user
- Resolved energy and macronutrients for that quantity, stored as values on the entry
- `source_kind`: one of `barcode`, `database_search`, `custom_food`, `recipe`, `manual_estimate`
- `confidence`: `measured` or `estimated`. Manual entries default to `estimated`. Reports must be able to separate the two.
- `origin_ref`: a nullable soft reference to the food or recipe it came from, for convenience only. Nothing on a read path may depend on it resolving.
- `unit_cost` and `currency`, nullable, snapshotted the same way as nutrition

### Food record

- Add `price_per_unit` and `currency`, both nullable
- Do not build cost reporting in Phase 1. The columns exist so that the migration is not needed later.

### Recipe

- A template only. Composed of food references and quantities.
- Logging a recipe expands it into either one entry with summed snapshotted values, or one entry per component, at the user's choice. Default to summed. Store the recipe version or a content hash on the entry so a later diff is possible.

### Weight log

- **Deferred, 2026-08-11.** The owner will not log weight manually in this app. Weight data, if
  used at all, will come from Google Health Connect in a future phase. No `WeightLog` entity
  exists and none is proposed by Phase 1.4. Phase 3.2 and 3.6 (weight trend) depend on a weight
  data source that is not yet decided; revisit before starting either.

### Migration policy

- Every migration ships with a test that loads a fixture database at version N, migrates, and asserts row counts and a sample of values are unchanged.
- No destructive migration without an automatic pre-migration export to the device's documents directory.

## 5. Features

### Phase 1: correctness of the foundation

**1.1 Entry snapshotting.** If entries currently resolve nutrition from a food reference, convert to snapshotting. Backfill existing entries from current food values, and mark backfilled entries with a flag so it is known they were reconstructed rather than captured.

**1.2 Provenance.** Implement `source_kind` and `confidence`. Surface `estimated` entries with a subtle visual marker in the diary.

**1.3 Cost columns.** Add the nullable columns to food records and entries. No UI.

**1.4 Export.** A single action producing a zip containing: full entry history as CSV, foods, and recipes. This must exist before any import work, so that a bad import is recoverable. Weight log is omitted per the deferral in §4 — there is no weight data to export.

Acceptance: schema documented, migration tests pass, export round-trips into a fresh install without loss.

### Phase 2: migration off MyFitnessPal

**2.1 Importer** for the MyFitnessPal Premium data export (three CSVs in a zip: nutrition detail with per-meal macros and timestamps, progress history, exercise history).

Behaviour:

- Parse the nutrition CSV into entries. MFP food identifiers do not map to Open Food Facts barcodes. Do not guess.
- Matching strategy, in order: exact name match against local custom foods, then fuzzy name match against the local Open Food Facts subset with a similarity threshold, then unmatched.
- Fuzzy matches above threshold are imported with `confidence = estimated` and flagged `needs_review`.
- Unmatched rows are imported as `manual_estimate` entries preserving the calorie and macro values from the CSV. Never drop a row.
- Import the progress CSV into the weight log.
- Ignore the exercise CSV in this phase.
- Produce `import-report.md`: counts by match category, and the full list of `needs_review` items.

**2.2 Review screen.** A list of `needs_review` entries with the ability to confirm, remap, or leave as estimated. Bulk-confirm by food name.

Acceptance: importing a real export produces zero dropped rows, a readable report, and daily calorie totals that match MFP's own totals for ten spot-checked days.

### Phase 3: the reason this fork exists

**3.1 Rolling weekly budget.** Replace or supplement the daily target with a seven-day rolling energy balance. Show cumulative surplus or deficit against the rolling window, and allow carryover so that a high day is absorbed rather than presented as a failure. The daily view remains available.

Configuration: target intake, window length (default 7 days), carryover on or off.

**3.2 Expenditure from observed data.** Estimate maintenance intake by regressing the weight trend against logged intake over a trailing window, rather than from an activity-multiplier formula. Requirements:

- Use a smoothed weight trend, not raw daily values
- Require a minimum window of logged data before displaying an estimate, and show nothing rather than a bad estimate
- Display the estimate with its uncertainty, and make clear it is derived from the user's own data
- Degrade gracefully when logging is sparse

**3.3 Meal templates and fast re-log.** A named, saved combination of entries that can be logged in one action. Surface the most frequently logged templates and foods on the diary screen so the two-tap criterion in Section 1 is met. Re-measure `docs/tap-audit.md` after this and include the before and after numbers.

**3.4 Calendar view.** Month grid, one cell per day, coloured by position against target. Tapping a day opens that day's diary. Long-press for a summary.

**3.5 Categorisation.** User-defined tags on foods and entries, freely assignable, filterable in reports.

**3.6 Weekly report.** For a selected week: intake against budget, rolling balance, macro distribution, tag breakdown, measured versus estimated proportion, weight trend. Exportable as markdown or CSV.

Acceptance: each feature has at least one test; the tap audit shows two taps or fewer for re-logging a frequent meal.

### Phase 4: cost tracking (only after Phase 3 is in daily use)

**4.1** Price entry on food records, per unit, with the unit matching how the food is logged.
**4.2** Cost snapshotting onto entries at log time.
**4.3** Cost per meal, per day, per week, and per tag in the weekly report.
**4.4** Entries with no known price are reported as an explicit unknown, never as zero.

### Phase 5: distribution (only after one month of the owner's daily use)

**5.1** Licence obligations satisfied: public source repository if required, notices preserved, distinct application ID and name so the fork cannot be confused with upstream.
**5.2** Google limited distribution account registered. Free, no government ID, up to 20 devices. Do not build a habit around unregistered APK sideloading, since developer verification is being enforced progressively from late 2026 and globally in 2027.
**5.3** First-run flow: seed the Open Food Facts subset, choose targets, optional MFP import.
**5.4** Local crash log capture with explicit user-initiated sharing. No automatic upload.
**5.5** `MAINTENANCE.md`: how to cut a release, how to test a migration against a real database, what to do when a friend reports data loss.

## 6. Open Food Facts

Do not depend on the live search API for the primary path.

- Ship or download on first run a filtered Open Food Facts subset: the owner's country, products with populated nutriment fields, into local SQLite with FTS5.
- Live API as fallback on barcode miss only. Send an identifying User-Agent.
- If the fork already has external database management and USDA integration, extend that mechanism rather than adding a parallel one.
- Keep the APK reasonable: download the subset on first run rather than bundling it, if size is a problem.

## 7. Non-goals

- Exercise tracking, beyond importing the MFP history for completeness
- Social features, sharing, streaks, gamification
- Cloud sync between devices
- iOS
- Photo recognition of meals
- Publishing to the Play Store or F-Droid
- Any nutrition advice, target recommendation, or coaching logic. The app records and reports. The user sets their own targets.

## 8. Definition of done, whole project

- The owner has logged every meal in this app and not in MyFitnessPal for 30 consecutive days
- The MyFitnessPal subscription is cancelled and its export is imported and reviewed
- A full export and reimport into a clean install loses nothing
- Every migration has a fixture test
- `CLAUDE.md`, `docs/schema.md`, and `MAINTENANCE.md` are current

## Appendix: session starters

- `Read PRD.md and execute Phase 0. Produce all five deliverables. Stop at the gate.`
- `Read PRD.md and docs/schema.md. Propose the Phase 1.1 migration. Do not implement it.`
- `Implement PRD 3.3. Re-run the tap audit and report before and after numbers with screenshots.`
- `Review the last three commits against PRD Section 1. Report any violation of the snapshot rule.`
