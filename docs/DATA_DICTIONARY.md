# Demo Data Dictionary

This is a concise guide to the primary MEW Logistics Readiness fields. Unless
explicitly marked as a public catalog attribute, values in the local and
synthetic demo environments are fictional demonstration data.

| Area | Field | Meaning |
| --- | --- | --- |
| Installation catalog | `installation_code` | Stable application identifier for an installation. |
| Installation catalog | `service_branch` | Service associated with the installation reference: `USN`, `USMC`, `USAF`, or `USA`. |
| Installation catalog | `catalog_source_id`, `catalog_source_url` | Identifier and public source used for the catalog assertion. |
| Installation catalog | `catalog_verified_on` | Date the catalog assertion was last verified. |
| Installation catalog | `catalog_confidence` | Confidence/classification of the catalog assertion; for example, `PUBLIC_ATTRIBUTED`. |
| Installation catalog | `public_association_type` | Nature of the source-supported relationship, such as platform homeport or EW mission support. |
| Catalog source | `source_id`, `title`, `source_url` | Stable source manifest identifier, source title, and direct public-reference URL. |
| Catalog source | `source_version`, `retrieved_on`, `license_notes` | Version/retrieval and reuse context retained for reproducible catalog review. |
| Catalog source | `review_status`, `last_reviewed_on`, `reviewer` | Governance state (`DRAFT`, `VERIFIED`, `STALE`, or `RETIRED`), most recent review date, and reviewer. |
| Catalog attribution | `fact_category`, `confidence`, `rationale`, `reviewer`, `reviewed_on` | Classification and review metadata for one source-supported installation, platform, or loadout assertion. |
| Personnel quality | `personnel_category`, `paygrade`, `functional_role`, `assigned_uic` | Fictional roster attributes validated for grade/category consistency, role coverage, and unit/installation alignment. |
| Installation catalog | `synthetic_operational_data` | Indicates that readiness, staffing, inventory, maintenance, and other operational values are fictional. |
| Organization | `uic` | Unit identification code used to associate units, personnel, and maintenance records. |
| Platform/loadout | `hull_number`, `vessel_name`, `ship_class`, `domain` | Platform identity/class and operating domain. These include maritime, airborne, subsurface, and ground examples. |
| Asset | `serial_number` | Demonstration equipment identifier; synthetic serials use the `SIM-` prefix. |
| Asset | `readiness_status` | Equipment condition: `FMC` (fully mission capable), `PMC` (partially mission capable), `NMCM` (not mission capable—maintenance), or `NMCS` (not mission capable—supply). |
| Asset | `operational_availability_pct` | Demonstration availability percentage for the assigned asset. |
| Work order | `jcn` | Job control number for a maintenance action form (MAF). |
| Work order | `status` | Lifecycle state, including `IN_WORK`, `AWAITING_PARTS`, `QA_PENDING`, and `CLOSED`. |
| Work order | `technician`, `qa_inspector` | Personnel assigned to perform maintenance and to complete independent QA review. |
| Work order | `maintenance_hours` | Labor time recorded for the maintenance action. |
| Inventory | `quantity_on_hand` | Count of a cataloged material available at an installation. |
| Inventory | `safety_stock`, `reorder_point` | Demonstration inventory thresholds used to identify supply risk. |
| Inventory ledger | `transaction_type` | Immutable stock movement, such as `RCT` (receipt) or `DED` (deduction). |
| Requisition | `document_number`, `priority`, `status` | Supply request identifier, urgency, and current supply state. |
| Audit | `event_type`, `decision`, `resource_target`, `reason` | Authorization or lifecycle event, its outcome, target, and retained rationale. |
| Readiness snapshot | `snapshot_date`, `fmc_assets`, `pmc_assets`, `nmc_assets` | Daily unit-level readiness trend values. `nmc_assets` is the count not mission capable. |

## Data classification rules

- Installation names and source URLs are catalog references; they are not a
  claim of a current mission, loadout, readiness state, or operational status.
- Personnel identities, authorization assignments, asset serials, maintenance,
  inventory, requisitions, audit logs, and readiness metrics in demo data are
  fictional.
- Demo identities and subjects are test fixtures. Never reuse them for real
  authentication or authorization.
- Any new catalog claim must include a source, verification date, and explicit
  confidence level before it is presented in the UI.
- Installations may reference one or more `CatalogSource` records. The legacy
  installation source fields remain available during the API transition.
- The versioned source-of-record for seeded catalog sources is
  `backend-java/src/main/resources/catalog-sources/manifest.csv`. Update it in
  the same change as any source-registry or seed-data update.
