# Licence

## What Food You is under

**GNU General Public License v3** (or, per the header, "at your option, any later version"). Full
text in `LICENSE` at repo root (unmodified, 35 KB, standard FSF GPLv3). Copyright notice, from
`README.md` §License:

```
Copyright (C) 2024-2026 Mateusz Maksimowicz
```

There are no per-file copyright headers in the source tree — the single top-level `LICENSE` file
plus the notice block in `README.md` constitute "preserving all notices" for this project.

## Forking is permitted; the maintainer's "no contributions" stance is separate from licence terms

The maintainer states (README, `docs/docs/contribute.md`) that he does **not accept code
contributions**, citing licensing and the fact that the app generates income. That is a policy
about what he will merge into *his* repository — it is not a licence restriction and does not
prevent forking. GPLv3 explicitly permits redistribution and modification by anyone, including
running a commercial or income-generating fork, as long as the obligations below are met. This
project is a fork under `tbobm/FoodYou` (`origin`), not a contribution back upstream, and needs no
permission from the maintainer beyond what GPLv3 already grants.

Separately, upstream's decision log
(`docs/development/decision-log/0004-close-source-development.md`) explains the maintainer now
develops privately and only publishes source for released versions, to reduce scraping. This
affects how easily *this* fork can rebase against upstream (only released snapshots are visible)
but imposes no obligation on us.

## Obligations that apply to this project

1. **Preserve notices.** Keep `LICENSE` and the README copyright/license block intact and
   unmodified in this fork. Do not remove or alter them.
2. **Source availability triggers on distribution, not on modification.** GPLv3 §6 requires that
   anyone who receives a *distributed* binary (the APK) must also be able to get the complete
   corresponding source (this fork's source, as modified) under the same GPLv3 terms. Nothing is
   owed while the app is only built and run locally by the owner (PRD Phases 0–4). The obligation
   activates at **PRD Phase 5** (distribution to up to 20 friends).
3. **Before any Phase 5 distribution:**
   - Publish this fork's source (a public GitHub repo — `tbobm/FoodYou` already satisfies this,
     provided it stays public and current with whatever is distributed) or otherwise make the
     corresponding source available to recipients per GPLv3 §6 options.
   - State clearly that the app is modified and under GPLv3, and that recipients may redistribute
     under the same terms.
   - Ship the modified `LICENSE`/notices unchanged; add a note in the app or README that this is an
     unofficial fork, not the upstream Food You.
   - **Distinct application ID and app name** (PRD §5.1) — required so the fork isn't confused with
     the upstream app on recipients' devices (both practically, and this is standard fork etiquette
     though not itself a GPLv3 clause). This is a package-ID change and therefore a PRD §2 gate;
     defer the actual change until Phase 5 planning.
4. **No additional restrictions.** GPLv3 §10 forbids imposing further restrictions on downstream
   recipients' exercise of the licensed rights — e.g. Google Play Limited Distribution's "up to 20
   devices" cap (PRD §5.2) is a distribution *channel* limit chosen by the owner, not a licence
   term imposed on recipients, so it does not conflict.

## Nothing owed right now

Since the app has not yet been distributed to anyone, none of the Phase 5 obligations are active
yet. Phases 0–4 (local build, install on the owner's own device, MFP import) do not trigger GPLv3's
source-distribution requirement.
