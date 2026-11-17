# Curated Catalog Import and Reconciliation

`backend-java/src/main/resources/catalog-sources/manifest.csv` is the reviewed,
version-controlled registry of public sources used for catalog attribution. It
contains attribution metadata only; it is not a feed for operational,
personnel, readiness, or credential data.

## Review and import procedure

1. A curator verifies that a public source may be used, then updates the
   manifest with its stable `source_id`, HTTPS URL, retrieval date, version,
   reuse notes, reviewer, and one of `DRAFT`, `VERIFIED`, `STALE`, or `RETIRED`.
2. Submit the manifest change for review. `source_id` is stable and must never
   be reused for a different source.
3. Deploy the reviewed artifact and call the reconciliation endpoint in its
   default dry-run mode:

   `POST /api/v1/catalog-sources/reconcile`

4. Review the returned counts and `unlistedRegistryIds`. Unlisted entries are
   reported but never removed automatically because existing installation,
   platform, or loadout provenance can reference them.
5. After review, apply the source metadata upserts:

   `POST /api/v1/catalog-sources/reconcile?apply=true`

The endpoint requires the existing catalog-update permission. A successful
apply only adds new sources or updates metadata for manifest-listed IDs; it
does not delete sources or alter catalog-to-source associations.

## Manifest constraints

- Header and eight columns must exactly match the versioned template.
- Values are comma-delimited, so fields must not contain commas.
- IDs use uppercase letters, digits, and hyphens; each ID occurs once.
- URLs must be absolute HTTPS links and `retrieved_on` is ISO-8601 (`YYYY-MM-DD`).
- Invalid input is rejected before any database write.

Run the focused automated checks with:

```powershell
cd backend-java
mvn -Dtest=CatalogSourceManifestTest,CatalogSourceReconciliationServiceTest test
```
