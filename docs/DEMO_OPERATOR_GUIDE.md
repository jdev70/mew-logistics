# Demo Operator Guide

Use this guide to start a safe, reproducible MEW Logistics Readiness demo.
All synthetic scenarios use fictional personnel, logistics, maintenance,
readiness, and audit records. They must not be represented as live operational
data.

## Quick start: synthetic demonstration

From the repository root, run the default, disposable synthetic-data scenario:

```powershell
.\scripts\oracle\run-synthetic-data.ps1
```

The API starts at `http://localhost:8080`. This profile uses an in-memory H2
database, creates its schema at startup, and drops it when the process stops.
It never connects to the local Oracle database.

For the Angular client, use a second terminal:

```powershell
cd frontend
npm start
```

The client starts at `http://localhost:4200`. Stop either process with
`Ctrl+C`.

## Scenarios

The generator is deterministic for the configured seed. With the default seed
(`20261117`) and 180 days of history, the expected baseline records are:

| Record type | `demo` | `standard` (default) | `stress` |
| --- | ---: | ---: | ---: |
| Installations | 16 | 16 | 16 |
| Reviewed catalog sources | 4 | 4 | 4 |
| Units | 33 | 33 | 33 |
| Personnel | 46 | 136 | 361 |
| Platforms, assets, and loadout assignments | 13 each | 13 each | 13 each |
| Material catalog items | 20 | 60 | 160 |
| Inventory balances | 320 | 960 | 2,560 |
| Inventory transactions | 640 | 1,920 | 5,120 |
| Open work orders, requisitions, and work-order parts | 3 each | 3 each | 3 each |
| Audit events | 9 | 9 | 9 |
| Daily readiness snapshots | 5,940 | 5,940 | 5,940 |

The number of readiness snapshots is `33 × history-days`. Counts may differ
only if the generator implementation, seed, or history-days setting changes.

Use a smaller data set for a short walkthrough:

```powershell
.\scripts\oracle\run-synthetic-data.ps1 -Scenario demo -HistoryDays 30
```

Use the larger data set for table, filter, and pagination checks:

```powershell
.\scripts\oracle\run-synthetic-data.ps1 -Scenario stress -HistoryDays 365
```

To run Maven directly and specify a stable alternative seed:

```powershell
cd backend-java
mvn spring-boot:run "-Dspring-boot.run.profiles=synthetic-data" "-Dspring-boot.run.arguments=--mew.synthetic.seed=42 --mew.synthetic.scenario=demo --mew.synthetic.history-days=30"
```

## Profiles and data safety

| Profile | Database | Purpose | Safe reset |
| --- | --- | --- | --- |
| `synthetic-data` | In-memory H2 | Repeatable UI and interview demos | Stop and restart the process |
| `local` | File-backed H2 at `backend-java/data/mewdb` | Small local-development dataset | Stop the API, delete only `mewdb.mv.db`, then restart |
| `oracle-local` | Local Oracle XE | Oracle and Keycloak integration work | Do not reset without following the Oracle runbook |
| `prod` | Deployment-specific datasource | Production deployment | Never seed or reset from this guide |

The synthetic profile supports a demo authorization subject,
`demo:marcus-vance`, for the fictional Fleet Readiness Analyst. It is not a
credential, does not grant access outside the synthetic profile, and must not
be copied into a Keycloak or production identity mapping.

## Operator checks

After startup, confirm the following before a demonstration:

1. The API has started on port 8080 and the selected profile is
   `synthetic-data`.
2. The UI identifies the data as synthetic when that indicator is available.
3. Dashboard totals show assets across Navy, Marine Corps, Air Force, and Army
   installations.
4. Inventory includes both positive balances and constrained critical items.
5. Work orders include an in-work and an awaiting-parts example.

For the separate Oracle and Keycloak technician-to-QA lifecycle demonstration,
follow [TECHNICIAN_WORKORDER_DEMO.md](TECHNICIAN_WORKORDER_DEMO.md). That flow
uses the local Oracle profile and is not interchangeable with the disposable
synthetic-data profile.
