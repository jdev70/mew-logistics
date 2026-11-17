# Future Implementation Roadmap

This document records intentionally deferred improvements for MEW Logistics
Readiness. It is a planning aid, not a claim that these capabilities are already
implemented. The immediate demo should remain stable; larger refactors belong in
small, independently testable commits after the demo.

## Guiding principles

- Keep the product **joint-service**, even though its initial Java package and
  domain language originated at NSWC Crane.
- Keep public reference/catalog information separate from fictional operational
  records used for a demo.
- Prefer deterministic synthetic data: a known seed should always create the
  same demo scenario.
- Make every catalog assertion attributable to a public source, a verification
  date, and an explicit confidence level.
- Do not place credentials, Keycloak subjects, real personnel identities, or
  sensitive operational information in seed data or source control.

## Current baseline

The active synthetic-data profile currently provides:

- Joint-service installation, unit, platform/loadout, personnel, inventory,
  work-order, audit, and readiness demo records.
- Explicit installation-level provenance fields: source identifier, source URL,
  verification date, association type, confidence, and a synthetic-data flag.
- A visible UI distinction between public-source installation catalog data and
  synthetic operational data.
- A varied deterministic personnel-name pool. These identities are fictional
  and are not intended to resemble actual personnel rosters.

## Priority 1 — stabilize and document the demo

1. Add a short data dictionary for the catalog, work-order, inventory, audit,
   and analytics fields. **Completed:** see
   [DATA_DICTIONARY.md](DATA_DICTIONARY.md).
2. Document the supported synthetic-data scenarios (`demo`, `standard`, and
   `stress`), expected counts, and startup commands in one operator guide.
   **Completed:** see [DEMO_OPERATOR_GUIDE.md](DEMO_OPERATOR_GUIDE.md).
3. Add a dashboard banner showing the active environment/profile and synthetic
   data status, so a demonstration cannot be mistaken for production data.
4. Add automated smoke tests for the technician → QA → closed work-order
   lifecycle and for key analytics totals. **Completed:**
   `AuthorizationIntegrationTest` exercises the authorized HTTP lifecycle and
   reconciles analytics asset/FMC totals against persisted assets.

## Priority 2 — joint-service Java package refactor

### Why

The former Java package root, `mil.navy.nswc.mew`, accurately reflected
the project’s origin, but it can suggest that the implementation is Navy-only.
The data model and intended product scope are joint-service.

### Target

**Completed:** `mil.dod.mew` is the package root. It is concise and describes
the intended joint-service architecture without implying that a specific
installation owns the product.

### Safe implementation sequence

1. Move the source and test directory tree from `mil/navy/nswc/mew` to
   `mil/dod/mew` using a controlled repository move. **Completed.**
2. Update every Java `package` declaration and `import` reference.
   **Completed.**
3. Update Maven/Spring references, test package declarations, scripts, and
   documentation that use the former fully-qualified package name.
   **Completed where applicable.**
4. Run `mvn test`, the synthetic seed test, and the frontend production build.
5. Commit this as a mechanical refactor only. Do not mix feature or schema
   changes into the same commit.

### Non-goals

This rename should not change database table names, REST endpoint paths,
Keycloak realm configuration, or authorization behavior.

## Priority 3 — governed installation and platform catalog

1. Establish a versioned import manifest for every public source used by the
   catalog. Record source name, URL, retrieval date, license/reuse notes,
   source version, and reviewer. **Completed for the seeded source registry:**
   see `backend-java/src/main/resources/catalog-sources/manifest.csv`.
2. Introduce a dedicated `CatalogSource` entity rather than repeating source
   values on individual records. **Implemented for installations:** each
   installation, platform, and system-loadout can reference one or more
   reviewed sources.
3. Separate facts into three categories:
   - **Publicly attributed fact:** source supports the named installation,
     mission, platform, or association.
   - **Analyst-curated inference:** plausible relationship based on public
     information; show a confidence level and rationale.
   - **Synthetic operational record:** fictional readiness, personnel,
     maintenance, inventory, financial, and audit activity used for demos.
   **Implemented:** `CatalogAttribution` records a reviewed source, exactly one
   installation/platform/loadout target, fact category, confidence, rationale,
   reviewer, and review date. The synthetic fixture creates public-attribution
   records for its catalog targets while preserving its synthetic operational data.
4. Add review status values such as `DRAFT`, `VERIFIED`, `STALE`, and
   `RETIRED`, with a last-reviewed date and reviewer.
   **Implemented:** the manifest reconciliation service validates these values,
   reports a dry-run result, and only performs explicit metadata upserts.
5. Begin with a curated set of high-value MEW/EW-relevant installations and
   expand deliberately. Do not claim comprehensive coverage of all U.S.
   military installations without an authoritative, licensed source and a
   repeatable import process.

## Priority 4 — richer synthetic personnel model

1. Expand synthetic grade/rank distributions by service and personnel category
   (officer, enlisted, warrant officer where applicable, government civilian,
   and contractor when a scenario requires it).
2. Make specialties, roles, clearance eligibility, work centers, and unit
   assignments consistent with the installation and platform mission.
3. Model authorized absences, training currency, qualifications, and
   certifications as synthetic readiness inputs.
4. Keep generated names, IDs, email addresses, phone numbers, and biographies
   fictional. Never import real personnel data merely to make the demo feel
   authentic.
5. Add data-quality tests for name diversity, valid service/grade combinations,
   valid unit/installations relationships, and role coverage.
   **Implemented for synthetic data:** `SyntheticPersonnelDataQualityService`
   validates these rules against the seeded roster; the seed uses branch-appropriate
   enlisted rank labels for its fictional E-6 personnel.

## Priority 5 — lifecycle, logistics, audit, and analytics depth

1. Extend work-order states to capture assignment, maintenance in progress,
   technician completion, QA review, return-to-service, closure, and rework.
2. Tie work-order parts consumption and requisitions to the same asset,
   installation, and loadout context.
3. Generate audit ledger events for each meaningful lifecycle action, including
   actor, timestamp, authorization decision, before/after state, and rationale.
4. Add analytics for readiness, mean time to repair, maintenance backlog,
   parts fill rate, inventory turns, constrained-demand risk, labor hours, and
   synthetic cost trends.
5. Define reconciliation tests so dashboard totals equal the underlying work
   orders, requisitions, inventory transactions, and readiness snapshots.

## Recommended implementation order

1. Finish the interview/demo stabilization work.
2. Add data dictionary, scenario documentation, and lifecycle smoke tests.
3. Complete the package-root refactor in its own commit.
4. Normalize catalog provenance into a dedicated source model.
5. Enrich synthetic personnel and lifecycle data with data-quality tests.
6. Expand catalog coverage only through reviewed, attributable imports.

## Suggested commit boundaries

- `docs: add future implementation roadmap`
- `refactor: rename Java package root for joint-service architecture`
- `feat: normalize catalog source provenance`
- `feat: enrich deterministic synthetic personnel roster`
- `feat: extend maintenance lifecycle analytics`

