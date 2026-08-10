# Phase 1.1 proposal: entry snapshotting

Per PRD §2 gate rules: this is a proposal only. Nothing in this document has been implemented.

## Finding

PRD 1.1 asks: *"If entries currently resolve nutrition from a food reference, convert to
snapshotting. Backfill existing entries from current food values, and mark backfilled entries with
a flag so it is known they were reconstructed rather than captured."*

Per `docs/schema.md`, **entries do not resolve from a food reference today.** `MeasurementEntity`
(the diary log entry) points at `DiaryProductEntity`/`DiaryRecipeEntity` — independent value copies
created at logging time, with no foreign key back to the catalog `ProductEntity`/`RecipeEntity`.
Editing a catalog food cannot alter any existing diary entry. This is precisely the property PRD
1.1 requires.

## Recommended change: none

**No migration is proposed for the snapshot mechanism itself.** Per PRD §4's own framing ("If the
fork already has satisfies a requirement, say so and change nothing"), the honest answer here is
that 1.1's core ask is already met, upstream having done the equivalent work themselves in schema
version 25→26 (`unlinkDiaryMigration`, `app/src/androidMain/kotlin/.../migration/UnlinkDiaryMigration.android.kt`)
— which itself backfilled every then-existing `Measurement` row's linked product/recipe into a
fresh diary-local copy, before this fork's project began.

## The one open sub-requirement: the backfill flag

PRD 1.1 also asks that backfilled entries be flagged as reconstructed rather than captured. Two
separate cases fall under this, and they need different answers:

**1. Retroactively flagging what `unlinkDiaryMigration` already backfilled.** Not feasible, and not
recommended. That migration used plain `INSERT ... RETURNING id` to copy catalog rows
(`UnlinkDiaryMigration.android.kt` lines ~244–258) with no marker column and no separate audit
table recording which `DiaryProduct`/`DiaryRecipe` rows originated that way versus rows created
normally afterward (every diary log since then, including ones from before this fork, uses the
identical `insertProduct`/`insertRecipe` code path in `RoomFoodDiaryEntryRepository`). There is no
data left to distinguish the two populations. Proposing a migration to "flag" rows we cannot
actually identify would be cosmetic, not informative — recommend **not** doing this.

**2. Flagging *future* backfills**, if some later operation (e.g. a bug-fix migration, or a bulk
correction) needs to reconstruct entries from current food values again. This is a real, standing
need, but it doesn't require schema work *now* — it only needs to exist by the time such an
operation happens. Recommend deferring it to **PRD 1.2 (Provenance)**, which already requires adding
a `confidence` column (`measured`/`estimated`) to log entries. A future backfill operation can set
`confidence = estimated` on any row it reconstructs, which satisfies the *intent* of "know it was
reconstructed rather than captured" without a separate flag column. This keeps 1.1 and 1.2 as one
combined, smaller migration instead of two.

## If a schema change is still wanted, the smallest version

For completeness — not proposed, only sketched. If the owner disagrees with the recommendation
above and wants a marker anyway, the smallest workable change is a single nullable column:

```sql
ALTER TABLE Measurement ADD COLUMN reconstructedAt INTEGER DEFAULT NULL;
ALTER TABLE ManualDiaryEntry ADD COLUMN reconstructedAt INTEGER DEFAULT NULL;
```

- **Rollback path:** dropping a nullable additive column is safe; Room's default `AutoMigration`
  handles the add, a hand-written down-migration would `ALTER TABLE ... DROP COLUMN` (SQLite ≥3.35,
  confirm the app's bundled SQLite version before relying on this — Room's own migrations generally
  avoid `DROP COLUMN` and use the temp-table-rebuild pattern instead, as seen throughout
  `LegacyMigrations.kt`).
- **Data affected:** none — purely additive, defaults to `NULL` (not reconstructed) for all
  existing rows, which is accurate for every row logged through the normal path.
- **Fixture test:** per PRD §4's migration policy, would need a fixture-database test asserting row
  counts and the new column's default value are unchanged after migration, following the existing
  `Abstract*MigrationTest` pattern (`app/src/commonTest/.../migration/`).

## What this proposal does NOT cover

Cost columns (PRD 1.3) and full provenance (`source_kind`, `confidence`, `origin_ref`, PRD 1.2) are
separate, larger schema changes affecting both `MeasurementEntity` and `ManualDiaryEntryEntity` (and
`ProductEntity` for cost). They are out of scope for this 1.1-specific proposal and should be
proposed together as one combined migration when Phase 1.2/1.3 work begins, per the recommendation
above.
