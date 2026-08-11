# Phase 2.1 proposal: MyFitnessPal importer

Per PRD §2 gate rules: this is a proposal only. Nothing in this document has been implemented.

## One thing to resolve before this can be approved: I don't have a real MFP export to check column names against

PRD says the nutrition CSV has "per-meal macros and timestamps" but doesn't give exact column
headers, and I have no sample export in this repo. MFP's Premium data export format has changed
over the years and I don't want to guess and ship a parser that silently misreads columns —
that's exactly the "Do not guess" instruction the PRD gives for food matching, and it applies just
as much to parsing. **If you have a real export (even just the header row, redact the food/date
values if you want), that would let me get the column mapping right on the first try instead of
iterating against guesses.** Failing that, I'll build against the commonly-documented Premium
export column set and we treat the first real import as the actual test.

## Two conflicts with the PRD's literal wording, reported rather than forced

### 1. "Import the progress CSV into the weight log" — no weight log exists

Per your 2026-08-11 decision (`PRD.md` §4), weight tracking is deferred entirely — no `WeightLog`
entity, no in-app weight logging, possibly Health Connect in a future phase. PRD 2.1 asks this
importer to write the progress CSV into a weight log that by design does not exist yet.

**Recommendation:** parse the progress CSV enough to *count* rows and read date range, report that
count in `import-report.md` ("N weight entries found in your export, not imported — weight
tracking is deferred, see PRD §4"), and stop there. This satisfies "never drop a row" in spirit —
nothing is silently discarded, the user is told exactly what happened — without reopening the
weight-log decision or building a table to receive data that has nowhere real to go yet. When
weight tracking is eventually built, the raw progress CSV is still sitting in the original zip the
user keeps, so nothing is lost permanently either.

**If you'd rather reopen the weight-log decision now instead** (build a minimal table just so this
import has somewhere to land), say so — that would be a new gated schema change, separate from
everything else in this proposal.

### 2. "Flagged needs_review" — no such column exists, and I don't think one is needed

PRD's `Measurement`/`ManualDiaryEntry` columns from Phase 1.2 are `sourceKind` and `confidence`,
not a review flag. Adding a `needsReview BOOLEAN` column would be a new gated schema change. I
don't think it's necessary: `sourceKind` is a free-text column (no DB-level enum constraint), so a
fuzzy-matched row can get `sourceKind = 'mfp_fuzzy_match'` — a value outside PRD §4's original
five (`barcode`/`database_search`/`custom_food`/`recipe`/`manual_estimate`), added specifically to
mean "an MFP row that fuzzy-matched a catalog product and needs the user to confirm or remap it."
The Phase 2.2 review screen then just queries `WHERE sourceKind = 'mfp_fuzzy_match'` — no new
column, and confirming/remapping a row means updating `sourceKind` to whatever it's confirmed to
be. If you'd rather have an explicit boolean column instead of overloading `sourceKind`, say so —
it's a small, separate gated change.

## Import flow

1. User picks a `.zip` file via `ActivityResultContracts.OpenDocument()` (same picker pattern as
   `ImportCsvProductsScreen`, MIME `application/zip`).
2. Read the zip via `java.util.zip.ZipInputStream` (no existing precedent for reading an uploaded
   zip — export just added *writing* one — but it's the same standard library, no new dependency).
   Extract the three CSV entries by name.
3. Nutrition CSV → parsed with the existing RFC 4180 parser (`common/csv/CsvParser.kt` — already
   handles quoted fields with embedded commas, which real MFP notes/food names will have; no
   naive `split(",")`).
4. Progress CSV → counted only, per the weight-log conflict above.
5. Exercise CSV → ignored entirely, per PRD.
6. Matching, per row, in order:
   - Exact name match against `Product WHERE sourceType = 'User'` (local custom foods).
   - Fuzzy name match against `Product WHERE sourceType = 'OpenFoodFacts'` (the local OFF subset),
     scored by Levenshtein similarity ratio, threshold **0.85** (proposed, tune after a real
     import — happy to make this a settings value instead of a hardcoded constant if you'd
     rather adjust it without a rebuild).
     - Above threshold → insert a `Measurement` row, `confidence = 'estimated'`,
       `sourceKind = 'mfp_fuzzy_match'`, `originProductId` = the matched catalog product.
     - No candidate above threshold → unmatched.
   - Unmatched → insert a `ManualDiaryEntry` row (`sourceKind`/`confidence` default to
     `'manual_estimate'`/`'estimated'` already, per the Phase 1.2/1.3 migration), preserving the
     calorie/macro values straight from the CSV row. This is PRD's explicit "never drop a row"
     path — no food match needed, the numbers are just recorded as-is.
   - Exact match → insert a `Measurement` row, `confidence = 'measured'` (a real catalog food, not
     a guess), `sourceKind = 'mfp_import'` (again a free-text value outside the original five, but
     the closest honest description — it wasn't barcode-scanned or searched *in this app*, it was
     matched during import).
7. Levenshtein similarity: no existing fuzzy-matching code or dependency anywhere in the codebase.
   Proposing a plain ~20-line iterative edit-distance function local to this use case — not a
   shared utility yet, since nothing else needs it (ladder rung 6/7: smallest thing that works, no
   premature abstraction). No new dependency.
8. `import-report.md`: a single markdown string (counts by category: exact / fuzzy-needs-review /
   unmatched / progress-rows-skipped, plus the full list of `needs_review` items with their MFP
   name and matched-candidate name side by side), offered for save via
   `ActivityResultContracts.CreateDocument("text/markdown")` — same picker pattern as the Phase 1.4
   export, once import finishes. Not zipped; PRD asks for the one file.

## What this proposal does NOT cover

- Phase 2.2 (the review screen itself — confirm/remap/bulk-confirm UI). This proposal only get the
  imported rows into a state 2.2 can query (`sourceKind = 'mfp_fuzzy_match'`).
- Any UI for adjusting the 0.85 similarity threshold, unless you want that as a setting now rather
  than a constant.
- The weight log itself (see conflict #1 above).
