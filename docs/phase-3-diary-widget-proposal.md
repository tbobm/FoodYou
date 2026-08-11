# Phase 3 — MyFitnessPal-style daily-summary widget

> GitHub issues are disabled on `tbobm/FoodYou`; this doc is the issue-equivalent scope record, following the repo convention (cf. `docs/phase-2.1-proposal.md`).

## Summary

Re-create the core MyFitnessPal home-screen **widget**: a read-only Android home-screen widget showing today's calories remaining and macro progress, tappable to open the app. This is the single highest-value MFP widget; MFP's other widgets (quick-add, water) are out of scope for this issue.

## Scope (this issue)

- One home-screen widget: **today's energy** (consumed / goal / remaining) + protein/carb/fat consumed-vs-goal.
- Read-only. Tapping the widget opens the app (`MainActivity`).
- Refresh on the platform update interval; no live per-entry push.

## Non-goals (deferred)

- **Tap-to-relog / quick-add from the widget** — needs write-capable widget actions (`PendingIntent` → diary write without opening the app). Bigger lift; separate issue if wanted. Could reuse the existing fast-relog / recently-used-amounts infra in the `food` package.
- Water / weight / exercise widgets (no data source for weight yet — see Phase 3.2 block).
- iOS widgets (app is Android-only build today).

## Technical approach

- **Jetpack Glance** (`androidx.glance:glance-appwidget`) — Compose-based widgets; fits the Compose-first codebase. **New dependency** (approved as part of this issue).
- Lives entirely in `app/src/androidMain` (widgets are platform-specific); no `commonMain` / shared-UI changes.
- **No schema change.** Data is read-only, reusing existing Koin-injected use cases:
  - `ObserveDiaryMealsUseCase.observe(date)` → sum `nutritionFacts` (same path as `GoalsViewModel`)
  - `GoalsRepository.observeDailyGoals(date)` for the targets
  - `DateProvider.observeDate()` for "today"
- DI is process-global via Koin (`initKoin` runs in `FoodYouApplication.onCreate`, same process as the widget receiver), so the widget resolves dependencies from `GlobalContext`.
- Files: `DiaryWidget` (GlanceAppWidget), `DiaryWidgetReceiver` (GlanceAppWidgetReceiver), `res/xml/*_info.xml` provider metadata, manifest `<receiver>`, one widget-label string, one Glance dep + version-catalog entry.

## Acceptance

- Widget can be added to the home screen and shows today's energy remaining + macros against the current goal.
- Values match the in-app Goals card for the same day.
- Tapping opens the app. Debug build assembles.

## Estimate

Read-only version: ~a few focused days, mostly Android glue. (Tap-to-relog version, if pursued later: ~1 week — write-capable widget actions.)
